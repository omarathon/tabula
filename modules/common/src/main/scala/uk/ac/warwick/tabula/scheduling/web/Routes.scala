package uk.ac.warwick.tabula.scheduling.web

import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/scheduling"

	object jobs {
		def list = context + "/sysadmin/jobs/list"
		def status(instance: JobInstance) = context + "/sysadmin/jobs/job-status?id=%s" format encoded(instance.id)
	}
}
