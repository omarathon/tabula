package uk.ac.warwick.tabula.admin.web

import uk.ac.warwick.tabula.data.model.{Route, Module, Department}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/admin"
	def home = context + "/"
	def masquerade = context + "/masquerade"

	object department {
		def apply(department: Department) = context + "/department/%s" format (encoded(department.code))

		def permissions(department: Department) = context + "/department/%s/permissions" format (encoded(department.code))

		def createSubDepartment(department: Department) = context + "/department/%s/subdepartment/new" format (encoded(department.code))
		def createModule(department: Department) = context + "/department/%s/module/new" format (encoded(department.code))

		def sortModules(department: Department) = context + "/department/%s/sort-modules" format (encoded(department.code))
		def sortRoutes(department: Department) = context + "/department/%s/sort-routes" format (encoded(department.code))
	}

	object module {
		def apply(module: Module) = department(module.department) + "#module-" + encoded(module.code)

		def permissions(module: Module) = context + "/module/%s/permissions" format (encoded(module.code))
	}

	object route {
		def apply(route: Route) = department(route.department) + "#route-" + encoded(route.code)

		def permissions(route: Route) = context + "/route/%s/permissions" format (encoded(route.code))
	}
}
