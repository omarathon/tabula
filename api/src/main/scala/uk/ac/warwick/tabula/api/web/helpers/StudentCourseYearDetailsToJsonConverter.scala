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

	def jsonStudentCourseYearDetailsObject(scyd: StudentCourseYearDetails, fieldRestriction: APIFieldRestriction): Map[String, Any] = Seq(
		str("sceSequenceNumber", scyd, "sceSequenceNumber", fieldRestriction),
		int("yearOfStudy", scyd, "yearOfStudy", fieldRestriction),
		str("studyLevel", scyd, "studyLevel", fieldRestriction),
		boolean("casUsed", scyd, "casUsed", fieldRestriction),
		boolean("tier4Visa", scyd, "tier4Visa", fieldRestriction),
		str("academicYear", scyd, "academicYear.toString", fieldRestriction),
		fieldRestriction.nested("route").flatMap { restriction =>
			if (canViewProperty(scyd, "route"))
				Some("route", routeToJson(scyd.route, restriction))
			else None
		},
		fieldRestriction.nested("enrolmentStatus").flatMap { restriction =>
			if (canViewProperty(scyd, "enrolmentStatus"))
				Some("enrolmentStatus", sitsStatusToJson(scyd.enrolmentStatus, restriction))
			else None
		},
		fieldRestriction.nested("enrolmentDepartment").flatMap { restriction =>
			if (canViewProperty(scyd, "enrolmentDepartment"))
				Some("enrolmentDepartment", departmentToJson(scyd.enrolmentDepartment, restriction))
			else None
		},
		fieldRestriction.nested("modeOfAttendance").flatMap { restriction =>
			if (canViewProperty(scyd, "modeOfAttendance"))
				Some("modeOfAttendance", modeOfAttendanceToJson(scyd.modeOfAttendance, restriction))
			else None
		}
	).flatten.toMap

}
