package uk.ac.warwick.tabula.scheduling.helpers

import java.sql.ResultSet
import uk.ac.warwick.tabula.data.model.{BasicStudentCourseYearProperties, BasicStudentCourseProperties}
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportMemberHelpers._

/**
 *
 * Contains the data from the result set for the SITS query.  (Doesn't include member properties)
 */

class SitsStudentRow(resultSet: ResultSet)
	extends SitsStudentRowCourseDetails
	with SitsStudentRowYearDetails {

	def rs = resultSet

	val disabilityCode = resultSet.getString("disability")
}

// this trait holds data from the result set which will be used by ImportStudentCourseCommand to create
// StudentCourseDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowCourseDetails
	extends BasicStudentCourseProperties
{
	def rs: ResultSet

	var routeCode = rs.getString("route_code")
	var courseCode = rs.getString("course_code")
	var sprStatusCode = rs.getString("spr_status_code")
	var scjStatusCode = rs.getString("scj_status_code")
	var departmentCode = rs.getString("department_code")
	var awardCode = rs.getString("award_code")

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	var tutorUniId = rs.getString("spr_tutor1")

	// this is the key and is not included in StudentCourseProperties, so just storing it in a var:
	var scjCode: String = rs.getString("scj_code")

	// now grab data from the result set into properties
	this.mostSignificant = rs.getString("most_signif_indicator") match {
		case "Y" | "y" => true
		case _ => false
	}

	this.sprCode = rs.getString("spr_code")
	this.beginDate = toLocalDate(rs.getDate("begin_date"))
	this.endDate = toLocalDate(rs.getDate("end_date"))
	this.expectedEndDate = toLocalDate(rs.getDate("expected_end_date"))
	this.courseYearLength = rs.getString("course_year_length")
	this.levelCode = rs.getString("level_code")
}

// this trait holds data from the result set which will be used by ImportStudentCourseYearCommand to create
// StudentCourseYearDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowYearDetails extends BasicStudentCourseYearProperties {
	def rs: ResultSet

	var enrolmentDepartmentCode: String = rs.getString("enrolment_department_code")
	var enrolmentStatusCode: String = rs.getString("enrolment_status_code")
	var modeOfAttendanceCode: String = rs.getString("mode_of_attendance_code")
	var academicYearString: String = rs.getString("sce_academic_year")
	var moduleRegistrationStatusCode: String = rs.getString("mod_reg_status")

	this.yearOfStudy = rs.getInt("year_of_study")
	//this.fundingSource = rs.getString("funding_source")
	this.sceSequenceNumber = rs.getInt("sce_sequence_number")
}
