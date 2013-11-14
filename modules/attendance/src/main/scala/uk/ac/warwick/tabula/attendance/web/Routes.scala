package uk.ac.warwick.tabula.attendance.web

import uk.ac.warwick.tabula.data.model._
import java.net.URLEncoder
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	private def encoded(string: String) = URLEncoder.encode(string, "UTF-8")
	def home = "/"

	object department {
		def view(department: Department) = "/%s" format encoded(department.code)
		def manage(department: Department) = "/manage/%s" format encoded(department.code)
	}

	object admin {
		def departmentPermissions(department: Department) = "/admin/department/%s/permissions" format encoded(department.code)
	}

	object profile {
		def apply() = "/profile"
		
		def apply(student: StudentMember, academicYear: AcademicYear) =
			"/profile/%s/%s" format(encoded(student.universityId), encoded(academicYear.startYear.toString))
	}

	object agent {
		def view(relationshipType: StudentRelationshipType) = "/agent/%s" format encoded(relationshipType.urlPart)
		def student(student: StudentMember, relationshipType: StudentRelationshipType) =
			"/agent/%s/%s" format(encoded(relationshipType.urlPart), encoded(student.universityId))
		def record(pointSet: MonitoringPointSet, studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType) =
			"/agent/%s/%s/%s" format(encoded(relationshipType.urlPart), encoded(studentCourseDetails.urlSafeId), encoded(pointSet.id))
	}
}
