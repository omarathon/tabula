package uk.ac.warwick.tabula.scheduling.commands.imports

import scala.Option.option2Iterable
import scala.collection.JavaConversions.{mapAsScalaMap, seqAsJavaList}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.{Daoisms, MemberDao, ModuleRegistrationDaoImpl, StudentCourseDetailsDao, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.ImportRowTracker
import uk.ac.warwick.tabula.scheduling.services.{AwardImporter, CourseImporter, MembershipInformation, ModeOfAttendanceImporter, ModuleRegistrationImporter, ProfileImporter, SitsAcademicYearAware, SitsStatusesImporter}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileIndexService, ProfileService, SmallGroupService, UserLookupService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.StudentMember

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms with SitsAcademicYearAware with TaskBenchmarking {

	type UniversityId = String

	PermissionCheck(Permissions.ImportSystemData)

	var madService = Wire.auto[ModuleAndDepartmentService]
	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]
	var userLookup = Wire.auto[UserLookupService]
	var moduleRegistrationImporter = Wire.auto[ModuleRegistrationImporter]
	var features = Wire.auto[Features]
	var moduleRegistrationDao = Wire.auto[ModuleRegistrationDaoImpl]
	var smallGroupService = Wire.auto[SmallGroupService]
	var profileIndexService = Wire.auto[ProfileIndexService]
	var memberDao = Wire.auto[MemberDao]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]
	var studentCourseYearDetailsDao = Wire.auto[StudentCourseYearDetailsDao]

	var deptCode: String = _

	val BatchSize = 100

	def applyInternal() {
		if (features.profiles) {
			benchmarkTask("Import members") {
				doMemberDetails(madService.getDepartmentByCode(deptCode))
			}
			logger.info("Import completed")
		}
	}

	/** Import basic info about all members in Membership, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */

	def doMemberDetails(department : Option[Department]) {
		logger.info("Importing member details")
		val importRowTracker = new ImportRowTracker
		val importStart = DateTime.now

		val departments = department match {
			case Some(d) => Seq(d)
			case None => madService.allDepartments
		}

		departments.foreach { department =>
			logSize(profileImporter.membershipInfoByDepartment(department)).grouped(BatchSize).zipWithIndex.toSeq.par.foreach { case (membershipInfos, batchNumber) =>
				benchmarkTask(s"Import member details for department=${department.code}, batch=#${batchNumber + 1}") {
					logger.info(s"Fetching user details for ${membershipInfos.size} ${department.code} usercodes from websignon")

					val users: Map[UniversityId, User] =
						benchmarkTask("Fetch user details") {
							membershipInfos.map { m =>
								val (usercode, warwickId) = (m.member.usercode, m.member.universityId)

								val user = userLookup.getUserByWarwickUniIdUncached(warwickId) match {
									case FoundUser(u) => u
									case _ => userLookup.getUserByUserId(usercode)
								}

								m.member.universityId -> user
							}.toMap
						}

					logger.info(s"Fetching member details for ${membershipInfos.size} ${department.code} members from Membership")
					val studentRowCommands = benchmarkTask("Fetch member details") {
						transactional() {
							profileImporter.getMemberDetails(membershipInfos, users, importRowTracker)
						}
					}

					logger.info("Updating students")
					benchmarkTask("Update students") {
					// each apply has its own transaction
						transactional() {
							studentRowCommands map { _.apply }
							session.flush()
						}
					}

					benchmarkTask("Update casUsed field on StudentCourseYearDetails records") {
						transactional() {
							updateCasUsed(studentRowCommands)
						}
					}

					benchmarkTask("Update module registrations and small groups") {
						transactional() {
							updateModuleRegistrationsAndSmallGroups(membershipInfos, users)
						}
					}
				}
			}
		}

		if (department.isEmpty)
			benchmarkTask("Stamp missing rows") {
				stampMissingRows(importRowTracker, importStart)
		}
	}

	def stampMissingRows(importRowTracker: ImportRowTracker, importStart: DateTime) {
		transactional() {
			logger.warn("Timestamping missing rows.  Found "
					+ importRowTracker.universityIdsSeen.size + " students, "
					+ importRowTracker.scjCodesSeen.size + " studentCourseDetails, "
					+ importRowTracker.studentCourseYearDetailsSeen.size + " studentCourseYearDetails.");

			// make sure any rows we've got for this student in the db which we haven't seen in this import are recorded as missing
			logger.warn("Timestamping missing students")
			memberDao.stampMissingFromImport(importRowTracker.newStaleUniversityIds, importStart)

			logger.warn("Timestamping missing studentCourseDetails")
			studentCourseDetailsDao.stampMissingFromImport(importRowTracker.newStaleScjCodes, importStart)

			logger.warn("Timestamping missing studentCourseYearDetails")
			studentCourseYearDetailsDao.stampMissingFromImport(importRowTracker.newStaleScydIds, importStart)
		}
	}

	def updateModuleRegistrationsAndSmallGroups(membershipInfo: Seq[MembershipInformation], users: Map[UniversityId, User]): Seq[ModuleRegistration] = {
		logger.info("Fetching module registrations")

		val importModRegCommands = benchmarkTask("Get module registrations details for users") {
			moduleRegistrationImporter.getModuleRegistrationDetails(membershipInfo, users)
		}

		logger.info("Saving or updating module registrations")

		val newModuleRegistrations = benchmarkTask("Save or update module registrations") {
			(importModRegCommands map {_.apply }).flatten
		}

		val usercodesProcessed: Seq[String] = membershipInfo map { _.member.usercode }

		logger.info("Removing old module registrations")

		benchmarkTask("Delete old module registrations") {
			deleteOldModuleRegistrations(usercodesProcessed, newModuleRegistrations)
		}

		session.flush
		session.clear

		newModuleRegistrations
	}

	// For each student in the batch, find out if they have used a
	// Confirmation of Acceptance to Study letter to obtain a visa.
	//
	// This is called after a batch of rows are processed because each SCYD record for a
	// student is updated with the same data, so it only needs to be done on a per-student
	// basis, not a per-row basis - but it can only be done once the SCYD records are
	// in place
	def updateCasUsed(rowCommands: Seq[ImportMemberCommand]){
		logger.info("Updating cas used statuses")

		// first need to get a list of members from the list of commands
		val members = rowCommands.flatMap(command => memberDao.getByUniversityId(command.universityId))

		members.map { member => member match {
				case student: StudentMember => {
					val importCasUsageForStudentCommand = ImportCasUsageForStudentCommand(student, getCurrentSitsAcademicYear)
					importCasUsageForStudentCommand.apply()
				}
				case _ =>
			}
		}

		session.flush
		session.clear
	}

	def refresh(member: Member) {
		transactional() {
			val warwickId = member.universityId
			val user = userLookup.getUserByWarwickUniIdUncached(member.universityId) match {
				case FoundUser(u) => u
				case _ => userLookup.getUserByUserId(member.userId)
			}

			val importRowTracker = new ImportRowTracker

			profileImporter.membershipInfoForIndividual(member) match {
				case Some(membInfo: MembershipInformation) => {

					// retrieve details for this student from SITS and store the information in Tabula
					val importMemberCommands = profileImporter.getMemberDetails(List(membInfo), Map(warwickId -> user), importRowTracker)
					if (importMemberCommands.isEmpty) logger.warn("Refreshing student " + membInfo.member.universityId + " but found no data to import.")
					val members = importMemberCommands map { _.apply }

					// update missingFromSitsSince field in this student's member and course records:
					updateMissingForIndividual(member, importRowTracker)

					session.flush

					updateCasUsed(importMemberCommands)

					// re-import module registrations and delete old module and group registrations:
					val newModuleRegistrations = updateModuleRegistrationsAndSmallGroups(List(membInfo), Map(warwickId -> user))

					// TAB-1435 refresh profile index
					profileIndexService.indexItemsWithoutNewTransaction(members.flatMap { m => profileService.getMemberByUniversityId(m.universityId) })

					for (thisMember <- members) session.evict(thisMember)
					for (modReg <- newModuleRegistrations) session.evict(modReg)

					logger.info("Data refreshed for " + member.universityId)
				}
				case None => logger.warn("Student is no longer in uow_current_members in membership - not updating")
			}
		}
	}

	/*
	 * Called after refreshing an individual student.
	 *
	 * For any rows for this member which were previously not seen in the import but have now re-emerged,
	 * record that fact by setting missingFromSitsImportSince to null.
	 *
	 * Also, for any rows for this member which were previously seen but which are now missing,
	 * record that fact by setting missingFromSiteImportSince to now.
	 */
	def updateMissingForIndividual(member: Member, importRowTracker: ImportRowTracker) {
		member match {
			case stu: StudentMember => {

				// update missingFromImportSince on member
				if (stu.missingFromImportSince != null && importRowTracker.universityIdsSeen.contains(stu.universityId)) {
					stu.missingFromImportSince = null
					memberDao.saveOrUpdate(stu)
				}
				else if (stu.missingFromImportSince == null && !importRowTracker.universityIdsSeen.contains(stu.universityId)) {
					var missingSince = stu.missingFromImportSince
					stu.missingFromImportSince = DateTime.now
					missingSince = stu.missingFromImportSince

					memberDao.saveOrUpdate(stu)
				}

				for (scd <- stu.freshOrStaleStudentCourseDetails) {

					// on studentCourseDetails
					if (scd.missingFromImportSince != null
							&& importRowTracker.scjCodesSeen.contains(scd.scjCode)) {
						scd.missingFromImportSince = null
						studentCourseDetailsDao.saveOrUpdate(scd)
					}
					else if (scd.missingFromImportSince == null
							&& !importRowTracker.scjCodesSeen.contains(scd.scjCode)) {
						scd.missingFromImportSince = DateTime.now
						studentCourseDetailsDao.saveOrUpdate(scd)
					}

					// and on studentCourseYearDetails
					for (scyd <- scd.freshOrStaleStudentCourseYearDetails) {
						val key = new StudentCourseYearKey(scd.scjCode, scyd.sceSequenceNumber)
						if (scyd.missingFromImportSince != null
								&& importRowTracker.studentCourseYearDetailsSeen.contains(key)) {
							scyd.missingFromImportSince = null
							studentCourseYearDetailsDao.saveOrUpdate(scyd)
						}
						else if (scyd.missingFromImportSince == null
								&& !importRowTracker.studentCourseYearDetailsSeen.contains(key)) {
							scyd.missingFromImportSince = DateTime.now
							studentCourseYearDetailsDao.saveOrUpdate(scyd)
						}
					}
				}
			}
			case _ => {}
		}
	}

	def deleteOldModuleRegistrations(usercodes: Seq[String], newModuleRegistrations: Seq[ModuleRegistration]) {
		val existingModuleRegistrations = moduleRegistrationDao.getByUsercodesAndYear(usercodes, getCurrentSitsAcademicYear)
		for (existingMR <- existingModuleRegistrations.filterNot(mr => newModuleRegistrations.contains(mr))) {
			existingMR.studentCourseDetails.moduleRegistrations.remove(existingMR)
			session.delete(existingMR)

			smallGroupService.removeFromSmallGroups(existingMR)
		}
	}

	def describe(d: Description) = d.property("deptCode" -> deptCode)
}
