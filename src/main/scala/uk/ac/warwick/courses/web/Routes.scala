package uk.ac.warwick.courses.web

import uk.ac.warwick.courses.data.model._
import java.net.URLEncoder

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 */
object Routes {
	private def encoded(string:String) = URLEncoder.encode(string, "UTF-8")
	
	def home = "/"
	def assignment(assignment:Assignment) = "/module/%s/%s/" format (encoded(assignment.module.code), encoded(assignment.id))
	
	object admin {
		def department(department:Department) = "/admin/department/%s/" format (encoded(department.code))
		def module(module:Module) = department(module.department) + "#module-" + encoded(module.code)
		def modulePermissions(module:Module) = "/admin/module/%s/permissions" format (encoded(module.code))
	}
}

object Mappings {
	object admin {
		final val permissions = "/admin/module/{module}/permissions"
	}
}