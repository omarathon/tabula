package uk.ac.warwick.tabula.sysadmin.web

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/sysadmin"

	object Departments {
		def home = context + "/departments"
		def department(department: Department) = context + "/departments/%s" format encoded(department.code)
	}

	object AttendanceTemplates {
		def home = context + "/attendancetemplates"
		def edit(template: AttendanceMonitoringTemplate) = context + "/attendancetemplates/%s/edit" format encoded(template.id)
	}

	object Relationships {
		def home = context + "/relationships"
	}
}
