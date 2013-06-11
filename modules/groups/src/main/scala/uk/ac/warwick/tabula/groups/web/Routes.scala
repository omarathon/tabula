package uk.ac.warwick.tabula.groups.web

import uk.ac.warwick.tabula.data.model._
import java.net.URLEncoder
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 * 
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	private def encoded(string: String) = URLEncoder.encode(string, "UTF-8")
	def home = "/"

	object admin {
		def apply(department: Department) = "/admin/department/%s" format (encoded(department.code))
		
		object module {
			def apply(module: Module) = admin(module.department) + "#module-" + encoded(module.code)
		}
		
		def allocate(set: SmallGroupSet) = "/admin/module/%s/groups/%s/allocate" format (encoded(set.module.code), encoded(set.id))
	}
}
