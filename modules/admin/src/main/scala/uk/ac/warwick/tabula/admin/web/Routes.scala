package uk.ac.warwick.tabula.admin.web

import uk.ac.warwick.tabula.data.model._
import java.net.URLEncoder

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
		def apply(department: Department) = "/department/%s" format (encoded(department.code))
		
		def permissions(department: Department) = "/department/%s/permissions" format (encoded(department.code))
		
		def createModule(department: Department) = "/department/%s/module/new" format (encoded(department.code))
		
		def sortModules(department: Department) = "/department/%s/sort-modules" format (encoded(department.code))
		def sortRoutes(department: Department) = "/department/%s/sort-routes" format (encoded(department.code))
	}
		
	object module {
		def apply(module: Module) = department(module.department) + "#module-" + encoded(module.code)
		
		def permissions(module: Module) = "/module/%s/permissions" format (encoded(module.code))
	}
}
