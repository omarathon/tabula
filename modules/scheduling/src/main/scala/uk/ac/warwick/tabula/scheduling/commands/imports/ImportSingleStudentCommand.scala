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

class ImportSingleStudentCommand(member: MembershipInformation, ssoUser: User, resultSet: ResultSet)
  extends ImportSingleMemberCommand(member, ssoUser, resultSet)
	with Logging with Daoisms with StudentProperties with StudyDetailsProperties with Unaudited {
	import ImportMemberHelpers._

	implicit val rs = resultSet
	implicit val metadata = rs.getMetaData

	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	var modeOfAttendanceImporter = Wire.auto[ModeOfAttendanceImporter]
	var profileService = Wire.auto[ProfileService]

	// A few intermediate properties that will be transformed later
	var studyDepartmentCode: String = _
	var routeCode: String = _
	var sprStatusCode: String = _
	var enrolmentStatusCode: String = _
	var modeOfAttendanceCode: String = _

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	var sprTutor1 = rs.getString("spr_tutor1")
	//var sprTutor2 = rs.getString("spr_tutor2")
	//var scjTutor1 = rs.getString("scj_tutor1")
	//var scjTutor2 = rs.getString("scj_tutor2")

	this.sprCode = rs.getString("spr_code")
	this.scjCode = rs.getString("scj_code")
	this.sitsCourseCode = rs.getString("sits_course_code")
	this.routeCode = rs.getString("route_code")
	this.studyDepartmentCode = rs.getString("study_department")
	this.yearOfStudy = rs.getInt("year_of_study")

	this.nationality = rs.getString("nationality")
	this.mobileNumber = rs.getString("mobile_number")

	this.intendedAward = rs.getString("award_code")
	this.beginDate = toLocalDate(rs.getDate("begin_date"))
	this.endDate = toLocalDate(rs.getDate("end_date"))

	this.expectedEndDate = toLocalDate(rs.getDate("expected_end_date"))

	this.fundingSource = rs.getString("funding_source")
	this.courseYearLength = rs.getString("course_year_length")

	this.sprStatusCode = rs.getString("spr_status_code")
	this.enrolmentStatusCode = rs.getString("enrolment_status_code")
	this.modeOfAttendanceCode = rs.getString("mode_of_attendance_code")

	override def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityId(universityId)

		logger.debug("Importing member " + universityId + " into " + memberExisting)

		val (isTransient, member) = memberExisting match {
			case Some(member: StudentMember) => (false, member)
			case Some(member: OtherMember) => {
				// TAB-692 delete the existing member, then return a brand new one
				memberDao.delete(member)
				(true, new StudentMember(universityId))
			}
			case Some(member) => throw new IllegalStateException("Tried to convert " + member + " into a student!")
			case _ => (true, new StudentMember(universityId))
		}

		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)
		val studyDetailsBean = new BeanWrapperImpl(member.studyDetails)

		// We intentionally use a single pipe rather than a double pipe here - we want both statements to be evaluated
		val hasChanged =
			copyMemberProperties(commandBean, memberBean) |
			copyStudentProperties(commandBean, memberBean) |
			copyStudyDetailsProperties(commandBean, studyDetailsBean)

		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)

			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}

		if (member.studyDetails == null) logger.warn("Can't capture tutor for student " + sprCode + "; studyDetails is null")
		else captureTutor(member.studyDetails.studyDepartment)

		new ImportSupervisorsForSingleStudentCommand(member).apply

		member
	}

	private val basicStudentProperties = Set(
		"nationality", "mobileNumber"
	)

	// We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean)

	private val basicStudyDetailsProperties = Set(
		"sprCode",
		"scjCode",
		"sitsCourseCode",
		"yearOfStudy",
		"intendedAward",
		"beginDate",
		"endDate",
		"expectedEndDate",
		"fundingSource",
		"courseYearLength",
		"modeOfAttendance"
		//,
		//"levelCode"

	)

	private def copyStudyDetailsProperties(commandBean: BeanWrapper, studyDetailsBean: BeanWrapper) = {
		copyBasicProperties(basicStudyDetailsProperties, commandBean, studyDetailsBean) |
		copyDepartment("studyDepartment", homeDepartmentCode, studyDetailsBean) |
		copyRoute("route", routeCode, studyDetailsBean) |
		copyStatus("sprStatus", sprStatusCode, studyDetailsBean) |
		copyStatus("enrolmentStatus", enrolmentStatusCode, studyDetailsBean) |
		copyModeOfAttendance("modeOfAttendance", modeOfAttendanceCode, studyDetailsBean)
	}

	private def copyStatus(property: String, code: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: SitsStatus => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no SPR status to having an SPR status
			memberBean.setPropertyValue(property, toSitsStatus(code))
			true
		} else if (code == null) {
			// User had an SPR status code but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			memberBean.setPropertyValue(property, toSitsStatus(code))
			true
		}
	}

	private def copyModeOfAttendance(property: String, code: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: ModeOfAttendance => value
		}

		if (oldValue == null && code == null) false
		else if (oldValue == null) {
			// From no MOA to having an MOA
			memberBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		} else if (code == null) {
			// User had an SPR status code but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == code.toLowerCase) {
			false
		}	else {
			memberBean.setPropertyValue(property, toModeOfAttendance(code))
			true
		}
	}

	def captureTutor(dept: Department) = {

		if (dept == null)
			logger.warn("Trying to capture tutor for " + sprCode + " but department is null.")

		// is this student in a department that is set to import tutor data from SITS?
		else if (dept.personalTutorSource != null && dept.personalTutorSource == Department.Settings.PersonalTutorSourceValues.Sits) {
			val tutorUniIdOption = getUniIdFromPrsCode(sprTutor1)

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

	private def getUniIdFromPrsCode(prsCode: String): Option[String] = {
		if (prsCode == null || prsCode.length() !=9) {
			None
		}
		else {
			val uniId = prsCode.substring(2)
			if (uniId forall Character.isDigit ) Some(uniId)
			else None
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

	private def toSitsStatus(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			sitsStatusesImporter.sitsStatusMap.get(code).getOrElse(null)
		}
	}

	private def toModeOfAttendance(code: String) = {
		if (code == null || code == "") {
			null
		} else {
			modeOfAttendanceImporter.modeOfAttendanceMap.get(code).getOrElse(null)
		}
	}

	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}
