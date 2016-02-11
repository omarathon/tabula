package uk.ac.warwick.tabula.scheduling.helpers

import java.sql.ResultSet
import uk.ac.warwick.tabula.data.model.{BasicStudentCourseYearProperties, BasicStudentCourseProperties}
import uk.ac.warwick.tabula.scheduling.commands.imports.ImportMemberHelpers._

trait HasResultSet {
	def rs: ResultSet
}

/**
 * Contains the data from the result set for the SITS query.  (Doesn't include member properties)
 */
class SitsStudentRow(val rs: ResultSet)
	extends SitsStudentRowCourseDetails
		with SitsStudentRowYearDetails
		with HasResultSet {

	val disabilityCode = rs.getString("disability")
	val deceased = rs.getString("mst_type") match {
		case "D" | "d" => true
		case _ => false
	}
}

// this trait holds data from the result set which will be used by ImportStudentCourseCommand to create
// StudentCourseDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowCourseDetails
	extends BasicStudentCourseProperties {
	self: HasResultSet =>

	var routeCode = rs.getString("route_code")
	var courseCode = rs.getString("course_code")
	var sprStatusCode = rs.getString("spr_status_code")
	var scjStatusCode = rs.getString("scj_status_code")
	var departmentCode = rs.getString("department_code")
	var awardCode = rs.getString("award_code")
	var sprStartAcademicYearString = rs.getString("spr_academic_year_start")

	// tutor data also needs some work before it can be persisted, so store it in local variables for now:
	//WMG uses a different table and column for their tutors
	var tutorUniId: String =
		if(departmentCode != null && departmentCode.toLowerCase == "wm") {
			if(rs.getString("scj_tutor1") != null) {
				rs.getString("scj_tutor1").substring(2)
			} else {
				rs.getString("scj_tutor1")
			}
		} else {
			rs.getString("spr_tutor1")
		}

	// this is the key and is not included in StudentCourseProperties, so just storing it in a var:
	var scjCode = rs.getString("scj_code")

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
	this.reasonForTransferCode = rs.getString("scj_transfer_reason_code")
}

// this trait holds data from the result set which will be used by ImportStudentCourseYearCommand to create
// StudentCourseYearDetails.  The data needs to be extracted in this command while the result set is accessible.
trait SitsStudentRowYearDetails extends BasicStudentCourseYearProperties {
	self: HasResultSet =>

	var enrolmentDepartmentCode = rs.getString("enrolment_department_code")
	var enrolmentStatusCode = rs.getString("enrolment_status_code")
	var modeOfAttendanceCode = rs.getString("mode_of_attendance_code")
	var academicYearString = rs.getString("sce_academic_year")
	var moduleRegistrationStatusCode = rs.getString("mod_reg_status")

	this.yearOfStudy = rs.getInt("year_of_study")
	//this.fundingSource = rs.getString("funding_source")
	this.sceSequenceNumber = rs.getInt("sce_sequence_number")
	this.agreedMark = Option(rs.getBigDecimal("sce_agreed_mark")).map(_.setScale(1, java.math.RoundingMode.HALF_UP)).orNull
}
