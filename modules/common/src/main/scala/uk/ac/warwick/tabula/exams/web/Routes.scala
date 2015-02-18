package uk.ac.warwick.tabula.exams.web

import uk.ac.warwick.tabula.data.model._

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import uk.ac.warwick.tabula.web.RoutesUtils._
	private val context = "/exams"
	def home = context + "/"

	object admin {
		def department(department: Department) = context + "/admin/department/%s/" format encoded(department.code)

		object module {
			def apply(module: Module) = department(module.adminDepartment) + "#module-" + encoded(module.code)
		}

	}

}
