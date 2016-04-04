package uk.ac.warwick.tabula.helpers.scheduling

import java.sql.ResultSet

import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers._
import uk.ac.warwick.tabula.data.model.{BasicStudentCourseProperties, BasicStudentCourseYearProperties}
import uk.ac.warwick.tabula.helpers.Logging

import scala.util.Try

trait HasResultSet {
	def resultSet: ResultSet
}

object SitsStudentRow {
	def apply(rs: ResultSet) = new SitsStudentRow(rs)
}

/**
 * Contains the data from the result set for the SITS query.
 */
class SitsStudentRow(val resultSet: ResultSet)
	extends SitsStudentRowCourseDetails
		with SitsStudentRowYearDetails
		with HasResultSet {

	implicit val rs = Option(resultSet)

	val universityId = optString("university_id")
	val usercode = optString("user_code")
	val title = optString("title")
	val preferredForename = optString("preferred_forename")
	val fornames = optString("forenames")
	val familyName = optString("family_name")
	val gender = optString("gender")
	val emailAddress = optString("email_address")
	val dateOfBirth = optLocalDate("date_of_birth")
	val inUseFlag = optString("in_use_flag")
	val alternativeEmailAddress = optString("alternative_email_address")
	val disabilityCode = optString("disability")
	val deceased = optString("mst_type") match {
		case Some("D") | Some("d") => true
		case _ => false
	}
	val nationality = optString("nationality")
	val mobileNumber = optString("mobile_number")
}

// this trait holds data from the result set which will be used by ImportStudentCourseCommand to create
// StudentCourseDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowCourseDetails
	extends BasicStudentCourseProperties {
	self: HasResultSet =>

	var routeCode = resultSet.getString("route_code")
	var courseCode = resultSet.getString("course_code")
	var sprStatusCode = resultSet.getString("spr_status_code")
	var scjStatusCode = resultSet.getString("scj_status_code")
	var departmentCode = resultSet.getString("department_code")
	var awardCode = resultSet.getString("award_code")
	var sprStartAcademicYearString = resultSet.getString("spr_academic_year_start")

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	//WMG uses a different table and column for their tutors
	var tutorUniId: String =
		if(departmentCode != null && departmentCode.toLowerCase == "wm") {
			if(resultSet.getString("scj_tutor1") != null) {
				resultSet.getString("scj_tutor1").substring(2)
			} else {
				resultSet.getString("scj_tutor1")
			}
		} else {
			resultSet.getString("spr_tutor1")
		}

	// this is the key and is not included in StudentCourseProperties, so just storing it in a var:
	var scjCode = resultSet.getString("scj_code")

	// now grab data from the result set into properties
	this.mostSignificant = resultSet.getString("most_signif_indicator") match {
		case "Y" | "y" => true
		case _ => false
	}

	this.sprCode = resultSet.getString("spr_code")
	this.beginDate = toLocalDate(resultSet.getDate("begin_date"))
	this.endDate = toLocalDate(resultSet.getDate("end_date"))
	this.expectedEndDate = toLocalDate(resultSet.getDate("expected_end_date"))
	this.courseYearLength = resultSet.getString("course_year_length")
	this.levelCode = resultSet.getString("level_code")
	this.reasonForTransferCode = resultSet.getString("scj_transfer_reason_code")
}

// this trait holds data from the result set which will be used by ImportStudentCourseYearCommand to create
// StudentCourseYearDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowYearDetails extends BasicStudentCourseYearProperties with Logging {
	self: HasResultSet =>

	var enrolmentDepartmentCode = resultSet.getString("enrolment_department_code")
	var enrolmentStatusCode = resultSet.getString("enrolment_status_code")
	var modeOfAttendanceCode = resultSet.getString("mode_of_attendance_code")
	var academicYearString = resultSet.getString("sce_academic_year")
	var moduleRegistrationStatusCode = resultSet.getString("mod_reg_status")

	var sceRouteCode = resultSet.getString("sce_route_code")

	this.yearOfStudy = resultSet.getInt("year_of_study")
	//this.fundingSource = rs.getString("funding_source")
	this.sceSequenceNumber = resultSet.getInt("sce_sequence_number")
	this.agreedMark = Try(resultSet.getBigDecimal("sce_agreed_mark")).toOption match {
		case Some(value) =>
			Option(value).map(_.setScale(1, java.math.RoundingMode.HALF_UP)).orNull
		case _ =>
			logger.error(s"Tried to import ${resultSet.getString("sce_agreed_mark")} as sce_agreed_mark for ${resultSet.getString("scj_code")}-$sceSequenceNumber but there was an error")
			null
	}
}
