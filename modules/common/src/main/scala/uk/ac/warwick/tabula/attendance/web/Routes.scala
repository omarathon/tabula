package uk.ac.warwick.tabula.attendance.web

import java.net.URLEncoder
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, StudentMember, Department}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/attendance"
	def home = context + "/"

	object department {
		def view(department: Department) = context + "/%s" format encoded(department.code)
		def viewPoints(department: Department) = context + "/view/%s/points" format encoded(department.code)
		def viewStudents(department: Department) = context + "/view/%s/students" format encoded(department.code)
		def viewStudent(department: Department, student: StudentMember) =
			context + "/view/%s/students/%s" format(encoded(department.code), encoded(student.universityId))
		def viewAgents(department: Department, relationshipType: StudentRelationshipType) =
			context + "/view/%s/agents/%s" format(encoded(department.code), encoded(relationshipType.urlPart))
		def manage(department: Department) = context + "/manage/%s" format encoded(department.code)
	}

	object admin {
		def departmentPermissions(department: Department) = context + "/admin/department/%s/permissions" format encoded(department.code)
	}

	object profile {
		def apply() = context + "/profile"

		def apply(student: StudentMember, academicYear: AcademicYear) =
			context + "/profile/%s/%s" format(encoded(student.universityId), encoded(academicYear.startYear.toString))
	}

	object agent {
		def view(relationshipType: StudentRelationshipType) = context + "/agent/%s" format encoded(relationshipType.urlPart)
		def student(student: StudentMember, relationshipType: StudentRelationshipType) =
			context + "/agent/%s/%s" format(encoded(relationshipType.urlPart), encoded(student.universityId))
	}
}
