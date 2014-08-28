package uk.ac.warwick.tabula.sysadmin.web

import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import uk.ac.warwick.tabula.web.RoutesUtils
import uk.ac.warwick.tabula.services.jobs.JobInstance

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/sysadmin"

	object AttendanceTemplates {
		def list = context + "/attendancetemplates"
		def edit(template: AttendanceMonitoringTemplate) = context + "/attendancetemplates/%s/edit" format encoded(template.id)
	}
}
