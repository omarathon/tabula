package uk.ac.warwick.tabula.sysadmin.web

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

	object jobs {
		def list = context + "/jobs/list"
		def status(instance: JobInstance) = context + "/jobs/job-status?id=%s" format encoded(instance.id)
	}

	object AttendanceTemplates {
		def list = context + "/attendancetemplates"
	}
}
