package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfigurationComponent

/**
	* We use Freemarker to ensure that we use the same logic as in ScalaBeansWrapper, i.e. that we don't expose
	* any properties that the user doesn't have permissions to see.
	*/
trait StudentCourseYearDetailsToJsonConverter extends MemberApiFreemarkerHelper {
	self: ScalaFreemarkerConfigurationComponent =>

	def jsonStudentCourseYearDetailsObject(scyd: StudentCourseYearDetails): Map[String, Any] = Seq(
		str("sceSequenceNumber", scyd, "sceSequenceNumber"),
		int("yearOfStudy", scyd, "yearOfStudy"),
		boolean("casUsed", scyd, "casUsed"),
		boolean("tier4Visa", scyd, "tier4Visa"),
		str("academicYear", scyd, "academicYear.toString"),
		if (canViewProperty(scyd, "route"))
			Some("route", routeToJson(scyd.route))
		else None,
		if (canViewProperty(scyd, "enrolmentStatus"))
			Some("enrolmentStatus", sitsStatusToJson(scyd.enrolmentStatus))
		else None,
		if (canViewProperty(scyd, "enrolmentDepartment"))
			Some("enrolmentDepartment", departmentToJson(scyd.enrolmentDepartment))
		else None,
		if (canViewProperty(scyd, "modeOfAttendance"))
			Some("modeOfAttendance", modeOfAttendanceToJson(scyd.modeOfAttendance))
		else None
	).flatten.toMap

}
