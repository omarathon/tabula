package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.AlumniProperties
import uk.ac.warwick.tabula.data.model.StudentProperties
import uk.ac.warwick.tabula.data.model.MemberProperties
import uk.ac.warwick.tabula.data.model.StaffProperties
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import java.sql.ResultSet
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.Gender
import uk.ac.warwick.tabula.data.model.MemberUserType
import java.sql.Blob
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.Transactions._
import java.sql.Date
import scala.reflect.BeanProperty
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MemberDao
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.BeanWrapper
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.Closeables._
import java.io.InputStream
import org.apache.commons.codec.digest.DigestUtils
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.actions.Sysadmin

class ImportSingleStudentCommand(val rs: ResultSet) extends ImportSingleMemberCommand(rs)
	with Logging with Daoisms with StudentProperties {
	
	// A couple of intermediate properties that will be transformed later
	@BeanProperty var studyDepartmentCode: String = _
	@BeanProperty var routeCode: String = _
	
	this.sprCode = rs.getString("spr_code")
	this.sitsCourseCode = rs.getString("sits_course_code")
	
	this.routeCode = rs.getString("route_code")
	
	this.yearOfStudy = rs.getInt("year_of_study")
	this.attendanceMode = rs.getString("mode_of_attendance")
	this.studentStatus = rs.getString("student_status")
	this.fundingSource = rs.getString("source_of_funding")
	this.programmeOfStudy = rs.getString("programme_of_study")
	this.intendedAward = rs.getString("intended_award")
	
	this.academicYear = toAcademicYear(rs.getString("academic_year_code"))
	this.studyDepartmentCode = rs.getString("study_department")
	this.courseStartYear = toAcademicYear(rs.getString("course_start_year"))
	this.yearCommencedDegree = toAcademicYear(rs.getString("year_commenced_degree"))
	this.courseBaseYear = toAcademicYear(rs.getString("course_base_start_year"))
	this.courseEndDate = toLocalDate(rs.getDate("course_end_date"))
	this.transferReason = rs.getString("transfer_reason")
	this.beginDate = toLocalDate(rs.getDate("begin_date"))
	this.endDate = toLocalDate(rs.getDate("end_date"))
	this.expectedEndDate = toLocalDate(rs.getDate("expected_end_date"))
	this.feeStatus = rs.getString("fee_status")
	this.domicile = rs.getString("domicile")
	this.highestQualificationOnEntry = rs.getString("highest_qualification_on_entry")
	this.lastInstitute = rs.getString("last_institute")
	this.lastSchool = rs.getString("last_school")
	
	override def applyInternal(): Member = transactional() {
		val memberExisting = memberDao.getByUniversityId(universityId)
		
		logger.debug("Importing member " + universityId + " into " + memberExisting)
		
		val isTransient = !memberExisting.isDefined
		val member = memberExisting getOrElse(new Member(universityId))
		
		val commandBean = new BeanWrapperImpl(this)
		val memberBean = new BeanWrapperImpl(member)
		
		// We intentionally use a single pipe rather than a double pipe here - we want both statements to be evaluated
		val hasChanged = copyMemberProperties(commandBean, memberBean) | copyStudentProperties(commandBean, memberBean)
			
		if (isTransient || hasChanged) {
			logger.debug("Saving changes for " + member)
			
			member.lastUpdatedDate = DateTime.now
			memberDao.saveOrUpdate(member)
		}
		
		member
	}

	private val basicStudentProperties = Set(
		"sprCode", "sitsCourseCode", "yearOfStudy", "attendanceMode", "studentStatus", 
		"fundingSource", "programmeOfStudy", "intendedAward", "academicYear", "courseStartYear",
		"yearCommencedDegree", "courseBaseYear", "courseEndDate", "transferReason", "beginDate",
		"endDate", "expectedEndDate", "feeStatus", "domicile", "highestQualificationOnEntry",
		"lastInstitute", "lastSchool"
	)
	
	// We intentionally use a single pipe rather than a double pipe here - we want all statements to be evaluated
	private def copyStudentProperties(commandBean: BeanWrapper, memberBean: BeanWrapper) =
		copyBasicProperties(basicStudentProperties, commandBean, memberBean) |
		copyRoute("route", routeCode, memberBean)
		
		private def copyRoute(property: String, routeCode: String, memberBean: BeanWrapper) = {
		val oldValue = memberBean.getPropertyValue(property) match {
			case null => null
			case value: Route => value
		}
		
		if (oldValue == null && routeCode == null) false
		else if (oldValue == null) {
			// From no route to having a route
			memberBean.setPropertyValue(property, toRoute(routeCode))
			true
		} else if (routeCode == null) {
			// User had a route but now doesn't
			memberBean.setPropertyValue(property, null)
			true
		} else if (oldValue.code == routeCode.toLowerCase) {
			false
		}	else {
			memberBean.setPropertyValue(property, toRoute(routeCode))
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
	
	override def describe(d: Description) = d.property("universityId" -> universityId).property("category" -> "student")

}