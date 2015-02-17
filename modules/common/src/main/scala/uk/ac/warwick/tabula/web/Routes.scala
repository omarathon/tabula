package uk.ac.warwick.tabula.web

import java.net.URLEncoder
import uk.ac.warwick.tabula.data.model.Assignment

object RoutesUtils {
	def encoded(string: String) = URLEncoder.encode(string, "UTF-8")
}

object Routes {

	import uk.ac.warwick.tabula
	val coursework = tabula.coursework.web.Routes
	val profiles   = tabula.profiles.web.Routes
	val groups     = tabula.groups.web.Routes
	val attendance = tabula.attendance.web.Routes
	val admin      = tabula.admin.web.Routes
	val sysadmin   = tabula.sysadmin.web.Routes
	val reports    = tabula.reports.web.Routes
	val scheduling = tabula.scheduling.web.Routes

}