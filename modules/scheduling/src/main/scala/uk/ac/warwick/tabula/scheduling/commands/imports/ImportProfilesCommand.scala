package uk.ac.warwick.tabula.scheduling.commands.imports

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.JavaConversions.seqAsJavaList
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.services.CourseImporter
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.scheduling.services.ModuleRegistrationImporter
import uk.ac.warwick.tabula.scheduling.services.ProfileImporter
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.scheduling.services.SitsAcademicYearAware
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.ModuleRegistrationDao
import uk.ac.warwick.tabula.data.ModuleRegistrationDaoImpl

class ImportProfilesCommand extends Command[Unit] with Logging with Daoisms with SitsAcademicYearAware {

	PermissionCheck(Permissions.ImportSystemData)

	var madService = Wire.auto[ModuleAndDepartmentService]
	var profileImporter = Wire.auto[ProfileImporter]
	var profileService = Wire.auto[ProfileService]
	var userLookup = Wire.auto[UserLookupService]
	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var modeOfAttendanceImporter = Wire.auto[ModeOfAttendanceImporter]
	var courseImporter = Wire.auto[CourseImporter]
	var moduleRegistrationImporter = Wire.auto[ModuleRegistrationImporter]
	var features = Wire.auto[Features]
	var moduleRegistrationDao = Wire.auto[ModuleRegistrationDaoImpl]

	val BatchSize = 250

	def applyInternal() {
		if (features.profiles) {
			benchmark("ImportMembers") {
				importSitsStatuses
				importModeOfAttendances
				courseImporter.importCourses
				doMemberDetails
			}
		}
	}

	def importSitsStatuses {
		logger.info("Importing SITS statuses")

		transactional() {
			sitsStatusesImporter.getSitsStatuses map { _.apply }

			session.flush
			session.clear
		}
	}

	def importModeOfAttendances {
		logger.info("Importing modes of attendance")

		transactional() {
			modeOfAttendanceImporter.getImportCommands foreach { _.apply() }

			session.flush
			session.clear
		}
	}

	/** Import basic info about all members in Membership, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */

	def doMemberDetails {
		benchmark("Import all member details") {
			for {
				department <- madService.allDepartments;
				userIdsAndCategories <- logSize(profileImporter.userIdsAndCategories(department)).grouped(BatchSize)
			} {
				logger.info("Fetching user details for " + userIdsAndCategories.size + " usercodes from websignon")
				val users: Map[String, User] = userLookup.getUsersByUserIds(userIdsAndCategories.map(x => x.member.usercode)).toMap

				transactional() {
					logger.info("Fetching member details for " + userIdsAndCategories.size + " members from Membership")
					profileImporter.getMemberDetails(userIdsAndCategories, users) map { _.apply }
					session.flush
					session.clear

					logger.info("Fetching module registrations")
					val importModRegCommands = moduleRegistrationImporter.getModuleRegistrationDetails(userIdsAndCategories, users)

					logger.info("Saving or updating module registrations")
					val newModuleRegistrations = (importModRegCommands map {_.apply }).flatten

					val usercodesProcessed: Seq[String] = userIdsAndCategories map { _.member.usercode }

					logger.info("Removing old module registrations")
					deleteOldModuleRegistrations(usercodesProcessed, newModuleRegistrations)
					session.flush
					session.clear
				}
			}
		}
	}

	def refresh(member: Member) {
		transactional() {
			val usercode = member.userId
			val user = userLookup.getUserByUserId(usercode)

			profileImporter.userIdAndCategory(member) match {
				case Some(membInfo: MembershipInformation) => {

					// retrieve details for this student from SITS and store the information in Tabula
					val importMemberCommands = profileImporter.getMemberDetails(List(membInfo), Map(usercode -> user))
					if (importMemberCommands.isEmpty) logger.warn("Refreshing student " + membInfo.member.universityId + " but found no data to import.")
					val members = importMemberCommands map { _.apply }
					session.flush
					for (member <- members) session.evict(member)

					// get the user's module registrations
					val importModRegCommands = moduleRegistrationImporter.getModuleRegistrationDetails(List(membInfo), Map(usercode -> user))
					if (importModRegCommands.isEmpty) logger.warn("Looking for module registrations for student " + membInfo.member.universityId + " but found no data to import.")
					val newModuleRegistrations = (importModRegCommands map { _.apply }).flatten
					deleteOldModuleRegistrations(Seq(usercode), newModuleRegistrations)
					session.flush
					for (modReg <- newModuleRegistrations) session.evict(modReg)

				}
				case None => logger.warn("Student is no longer in uow_current_members in membership - not updating")
			}
		}
	}

	def deleteOldModuleRegistrations(usercodes: Seq[String], newModuleRegistrations: Seq[ModuleRegistration]) {
		val existingModuleRegistrations = moduleRegistrationDao.getByUsercodesAndYear(usercodes, getCurrentSitsAcademicYear)
		for (existingMR <- existingModuleRegistrations) {
			if (!newModuleRegistrations.contains(existingMR)) {
				session.delete(existingMR)
			}
		}
	}

	def equal(s1: Seq[String], s2: Seq[String]) =
		s1.length == s2.length && s1.sorted == s2.sorted

	def describe(d: Description) {

	}

}