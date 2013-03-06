package uk.ac.warwick.tabula.scheduling.commands.imports

import java.sql.Date
import java.sql.ResultSet
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.AlumniProperties
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberProperties
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.StaffProperties
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.StudyDetailsProperties
import uk.ac.warwick.tabula.data.model.StudyDetailsProperties
import uk.ac.warwick.tabula.helpers.Closeables._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.services.MembershipInformation
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.scheduling.services.SitsStatusesImporter

class ImportSingleStudentCommand(member: MembershipInformation, ssoUser: User, resultSet: ResultSet) extends ImportSingleMemberCommand(member, ssoUser, resultSet)
	with Logging with Daoisms with StudentProperties with StudyDetailsProperties with Unaudited {
	import ImportMemberHelpers._
	
	implicit val rs = resultSet
	implicit val metadata = rs.getMetaData
	
	var sitsStatusesImporter = Wire.auto[SitsStatusesImporter]
	
	// A few intermediate properties that will be transformed later
	@BeanProperty var studyDepartmentCode: String = _
	@BeanProperty var routeCode: String = _
	@BeanProperty var sprStatusCode: String = _
	@BeanProperty var enrolmentStatusCode: String = _
	
	this.sprCode = rs.getString("spr_code")
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
	
	this.modeOfAttendance = rs.getString("mode_of_attendance")
	this.ugPg = rs.getString("ug_pg")
	
	override def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityId(universityId)
		
		logger.debug("Importing member " + universityId + " into " + memberExisting)
		
		val isTransient = !memberExisting.isDefined
		
		val member = memberExisting match {
			case Some(member: StudentMember) => member
			case Some(member) => throw new IllegalStateException("Tried to convert " + member + " into a student!")
			case _ => new StudentMember(universityId)
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
		"sitsCourseCode", 
		"yearOfStudy",
		"intendedAward",
		"beginDate",
		"endDate",
		"expectedEndDate",
		"fundingSource",
		"courseYearLength",
		"modeOfAttendance",
		"ugPg"
		//,
		//"levelCode"
		
	)
		
	private def copyStudyDetailsProperties(commandBean: BeanWrapper, studyDetailsBean: BeanWrapper) =
		copyBasicProperties(basicStudyDetailsProperties, commandBean, studyDetailsBean) |
		copyDepartment("studyDepartment", homeDepartmentCode, studyDetailsBean) |
		copyRoute("route", routeCode, studyDetailsBean) |
		copyStatus("sprStatus", sprStatusCode, studyDetailsBean) |
		copyStatus("enrolmentStatus", enrolmentStatusCode, studyDetailsBean)

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
	
	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")
}
