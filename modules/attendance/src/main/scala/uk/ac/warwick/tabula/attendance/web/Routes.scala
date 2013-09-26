package uk.ac.warwick.tabula.attendance.web

import uk.ac.warwick.tabula.data.model._
import java.net.URLEncoder
import uk.ac.warwick.tabula.AcademicYear
import scala.collection.JavaConverters._

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	private def encoded(string: String) = URLEncoder.encode(string, "UTF-8")
	def home = "/"
	object managingDepartment {
		def apply(department: Department) = "/manage/%s" format encoded(department.code)
	}

	def monitoringPoints = "/monitoringpoints"

	object admin {
		def departmentPermissions(department: Department) = "/admin/department/%s/permissions" format encoded(department.code)
	}

	object profile {
		def apply(scd: StudentCourseDetails, academicYear: AcademicYear) =
			"/profile/%s/%s" format(encoded(scd.urlSafeId), encoded(academicYear.startYear.toString))
	}
}
