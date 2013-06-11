package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.Date
import java.sql.ResultSet
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.AlumniProperties
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberProperties
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.model.OtherMember
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.RelationshipType.Supervisor
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.model.StaffProperties
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.data.model.StudyDetailsProperties
import uk.ac.warwick.tabula.data.model.StudyDetailsProperties
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.scheduling.services.ModeOfAttendanceImporter
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.scheduling.helpers.PropertyCopying
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentCourseProperties
import uk.ac.warwick.tabula.data.model.StudentCourseYearProperties
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.scheduling.helpers.SitsPropertyCopying
import uk.ac.warwick.tabula.data.model.StudentCourseProperties
import uk.ac.warwick.tabula.PrsCode

class ImportSingleStudentCourseCommand(stuMem: StudentMember, resultSet: ResultSet)
	extends Command[StudentMember] with Logging with Daoisms
	with StudentCourseProperties with Unaudited with PropertyCopying with SitsPropertyCopying{

	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]
	var memberDao = Wire.auto[MemberDao]

	import ImportMemberHelpers._

	implicit val rs = resultSet
	implicit val metadata = rs.getMetaData

	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var profileService = Wire.auto[ProfileService]
	var studentCourseDetailsDao = Wire.auto[StudentCourseDetailsDao]

	var scjCode: String = rs.getString("scj_code")

	// A few intermediate properties that will be transformed later
	var routeCode: String = _
	var sprStatusCode: String = _
	var departmentCode: String = _

	this.routeCode = rs.getString("route_code")
	this.departmentCode = rs.getString("department_code")

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	var sprTutor1 = rs.getString("spr_tutor1")

	this.sprCode = rs.getString("spr_code")
	this.courseCode = rs.getString("course_code")
	this.awardCode = rs.getString("award_code")
	this.beginDate = toLocalDate(rs.getDate("begin_date"))
	this.endDate = toLocalDate(rs.getDate("end_date"))
	this.expectedEndDate = toLocalDate(rs.getDate("expected_end_date"))
	this.courseYearLength = rs.getString("course_year_length")

	this.sprStatusCode = rs.getString("spr_status_code")

	override def applyInternal(): StudentCourseDetails = transactional() {
		val studentCourseDetailsExisting = studentCourseDetailsDao.getByScjCode(scjCode)

		logger.debug("Importing student course details for " + scjCode)

		val (isTransient, studentCourseDetails) = studentCourseDetailsExisting match {
			case Some(studentCourseDetails: StudentCourseDetails) => (false, studentCourseDetails)
			case _ => (true, new StudentCourseDetails(stuMem, scjCode))
		}

		val commandBean = new BeanWrapperImpl(this)
		val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

		val hasChanged = copyStudentCourseProperties(commandBean, studentCourseDetailsBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + studentCourseDetails)

			studentCourseDetails.lastUpdatedDate = DateTime.now
			studentCourseDetailsDao.saveOrUpdate(studentCourseDetails)
		}

		new ImportSingleStudentCourseYearCommand(studentCourseDetails: StudentCourseDetails, resultSet: ResultSet).apply

		captureTutor(department)

		new ImportSupervisorsForSingleStudentCommand(studentCourseDetails).apply

		studentCourseDetails
	}

	private val basicStudentCourseProperties = Set(
		"sprCode",
		"scjCode",
		"courseCode",
		"yearOfStudy",
		"intendedAward",
		"beginDate",
		"endDate",
		"expectedEndDate",
		"fundingSource",
		"courseYearLength"
	)

	private def copyStudentCourseProperties(commandBean: BeanWrapper, studyDetailsBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseProperties, commandBean, studyDetailsBean) |
		copyDepartment("department", departmentCode, studyDetailsBean) |
		copyRoute("route", routeCode, studyDetailsBean) |
		copyStatus("sprStatus", sprStatusCode, studyDetailsBean)
	}


	def captureTutor(dept: Department) = {

		if (dept == null)
			logger.warn("Trying to capture tutor for " + sprCode + " but department is null.")

		// is this student in a department that is set to import tutor data from SITS?
		else if (dept.personalTutorSource != null && dept.personalTutorSource == Department.Settings.PersonalTutorSourceValues.Sits) {
			val tutorUniIdOption = PrsCode.getUniversityId(sprTutor1)

			tutorUniIdOption match {
				case Some(tutorUniId: String) => {
					// only save the personal tutor if we can match the ID with a staff member in Tabula
					val member = memberDao.getByUniversityId(tutorUniId) match {
						case Some(mem: Member) => {
							logger.info("Got a personal tutor from SITS! SprCode: " + sprCode + ", tutorUniId: " + tutorUniId)

							val currentRelationships = profileService.findCurrentRelationships(PersonalTutor, sprCode)

							// Does this relationship already exist?
							currentRelationships.find(_.agent == tutorUniId) match {
								case Some(existing) => existing
								case _ => {
									// End all existing relationships
									currentRelationships.foreach { rel =>
										rel.endDate = DateTime.now
										profileService.saveOrUpdate(rel)
									}

									// Save the new one
									val rel = profileService.saveStudentRelationship(PersonalTutor, sprCode, tutorUniId)

									rel
								}
							}
						}
						case _ => {
							logger.warn("SPR code: " + sprCode + ": no staff member found for PRS code " + sprTutor1 + " - not importing this personal tutor from SITS")
						}
					}
				}
				case _ => logger.warn("Can't parse PRS code " + sprTutor1 + " for student " + sprCode)

			}
		}
	}

	private def copyRoute(property: String, code: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: Route => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no route to having a route
			memberBean.setPropertyValue(property, toRoute(code))
			true
		} else if (code == null) {
			// User had a route but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			memberBean.setPropertyValue(property, toRoute(code))
			true
		}
	}

	private def toRoute(routeCode: String) = {
		if (routeCode == null || routeCode == "") {
			null
		} else {
			moduleAndDepartmentService.getRouteByCode(routeCode.toLowerCase).getOrElse(null)
		}
	}

	override def describe(d: Description) = d.property("scjCode" -> scjCode).property("category" -> "student")
}

