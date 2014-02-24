package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.ResultSet
import org.hibernate.exception.ConstraintViolationException
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import ImportMemberHelpers.{opt, toLocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Unaudited}
import uk.ac.warwick.tabula.data.{Daoisms, MemberDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{CourseType, Department, Member, StudentCourseDetails, StudentCourseProperties, StudentMember, StudentRelationshipSource}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{ImportCommandFactory, ImportRowTracker, PropertyCopying}
import uk.ac.warwick.tabula.scheduling.services.{AwardImporter, CourseImporter}
import uk.ac.warwick.tabula.services.{CourseAndRouteService, RelationshipService}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.commands.Description

class ImportStudentCourseCommand(row: SitsStudentRow, stuMem: StudentMember, importCommandFactory: ImportCommandFactory)
	extends Command[StudentCourseDetails] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	var memberDao = Wire.auto[MemberDao]
	var relationshipService = Wire.auto[RelationshipService]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]
	var courseAndRouteService = Wire.auto[CourseAndRouteService]
	var courseImporter = Wire.auto[CourseImporter]
	var awardImporter = Wire.auto[AwardImporter]

	override def applyInternal(): StudentCourseDetails = {
		val studentCourseDetailsExisting = studentCourseDetailsDao.getByScjCodeStaleOrFresh(row.scjCode)

		logger.debug("Importing student course details for " + row.scjCode)

		val (isTransient, studentCourseDetails) = studentCourseDetailsExisting match {
			case Some(studentCourseDetails: StudentCourseDetails) => (false, studentCourseDetails)
			case _ => (true, new StudentCourseDetails(stuMem, row.scjCode))
		}

		val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

		val hasChanged = copyStudentCourseProperties(new BeanWrapperImpl(row), studentCourseDetailsBean) | markAsSeenInSits(studentCourseDetailsBean)

		if (isTransient || hasChanged) {
			try {
				logger.debug("Saving changes for " + studentCourseDetails)

				if (row.mostSignificant) {
					stuMem.mostSignificantCourse = studentCourseDetails
					logger.debug("Updating member most significant course to "+ studentCourseDetails +" for " + stuMem)
				}

				studentCourseDetails.lastUpdatedDate = DateTime.now
				studentCourseDetailsDao.saveOrUpdate(studentCourseDetails)
			}
			catch  {
				case exception: ConstraintViolationException => {
					logger.warn("Couldn't update course details for SCJ "
							+ studentCourseDetails.scjCode + ", SPR " + studentCourseDetails.sprCode
							+ ".  Might be invalid data in SITS - working on the assumption "
							+ "there shouldn't be multiple SPR codes for one current SCJ code")
					exception.printStackTrace
				}
			}
		}

		val studentCourseYearDetails = importCommandFactory.createImportStudentCourseYearCommand(row, studentCourseDetails).apply

		// Apply above will take care of the db.  This brings the in-memory data up to speed:
		studentCourseDetails.attachStudentCourseYearDetails(studentCourseYearDetails)

		// just check the SPR (status on route) code.  The SCJ code may indicate that they are
		// permanently withdrawn from the course, but it may have the same route code as their
		// current course (surprisingly).  In that case we don't want to go ahead and end all
		// relationships for the route code.
		if (row.sprStatusCode != null && row.sprStatusCode.startsWith("P")) {
			// they are permanently withdrawn
			endRelationships()
		}
		else {
			captureTutor(studentCourseDetails)

			if (row.scjCode != null && !row.scjStatusCode.startsWith("P"))
				new ImportSupervisorsForStudentCommand(studentCourseDetails).apply
		}

		importCommandFactory.rowTracker.scjCodesSeen.add(studentCourseDetails.scjCode)

		studentCourseDetails
	}

	private val basicStudentCourseProperties = Set(
		"sprCode",
		"scjCode",
		"beginDate",
		"endDate",
		"expectedEndDate",
		"courseYearLength",
		"mostSignificant",
		"levelCode"
	)

	private def copyStudentCourseProperties(rowBean: BeanWrapper, studentCourseDetailsBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseProperties, rowBean, studentCourseDetailsBean) |
		copyObjectProperty("department", row.departmentCode, studentCourseDetailsBean, toDepartment(row.departmentCode)) |
		copyObjectProperty("route", row.routeCode, studentCourseDetailsBean, toRoute(row.routeCode)) |
		copyObjectProperty("course", row.courseCode, studentCourseDetailsBean, toCourse(row.courseCode)) |
		copyObjectProperty("award", row.awardCode, studentCourseDetailsBean, toAward(row.awardCode)) |
		copyObjectProperty("statusOnRoute", row.sprStatusCode, studentCourseDetailsBean, toSitsStatus(row.sprStatusCode))
		copyObjectProperty("statusOnCourse", row.scjStatusCode, studentCourseDetailsBean, toSitsStatus(row.scjStatusCode))
	}

	def toRoute(routeCode: String) = routeCode match {
		case (code: String) => code.toLowerCase.maybeText.flatMap { courseAndRouteService.getRouteByCode }.getOrElse(null)
		case _ => null // catch case where route code is null
	}

	def toCourse(courseCode: String) = courseCode match {
		case (code: String) => code.maybeText.flatMap { courseImporter.getCourseForCode }.getOrElse(null)
		case _ => null
	}

	def toAward(awardCode: String) = awardCode match {
		case (code: String) => code.maybeText.flatMap { awardImporter.getAwardForCode }.getOrElse(null)
		case _ => null
	}

	def captureTutor(studentCourseDetails: StudentCourseDetails) = {
		val dept = studentCourseDetails.department
		if (dept == null)
			logger.warn("Trying to capture tutor for " + row.sprCode + " but department is null.")

		// Mark Hadley in Physics says "I don't think the University uses the term 'tutor' for PGRs"
		// so by default excluding PGRs from the personal tutor import:
		else if (row.courseCode != null && row.courseCode.length() > 0 && CourseType.fromCourseCode(row.courseCode) != CourseType.PGR) {
			// is this student in a department that is set to import tutor data from SITS?
			relationshipService
				.getStudentRelationshipTypeByUrlPart("tutor") // TODO this is awful
				.filter { relType => dept.getStudentRelationshipSource(relType) == StudentRelationshipSource.SITS }
				.foreach { relationshipType =>
					// only save the personal tutor if we can match the ID with a staff member in Tabula
					val member = memberDao.getByUniversityId(row.tutorUniId) match {
						case Some(mem: Member) => {
							logger.info("Got a personal tutor from SITS! SprCode: " + row.sprCode + ", tutorUniId: " + row.tutorUniId)

							relationshipService.replaceStudentRelationships(relationshipType, studentCourseDetails, Seq(mem))
						}
						case _ => {
							logger.warn("SPR code: " + row.sprCode + ": no staff member found for uni ID " + row.tutorUniId + " - not importing this personal tutor from SITS")
						}
					}
				}
		}
	}


	def endRelationships() {
		if (row.endDate != null) {
			val endDateFromSits = row.endDate.toDateTimeAtCurrentTime()
			val threeMonthsAgo = DateTime.now().minusMonths(3)
			if (endDateFromSits.isBefore(threeMonthsAgo)) {
				relationshipService.getAllCurrentRelationships(stuMem)
					.foreach { relationship =>
							relationship.endDate = endDateFromSits
							relationshipService.saveOrUpdate(relationship)
						}
			}
		}
	}

	override def describe(d: Description) = d.property("scjCode" -> row.scjCode)
}


