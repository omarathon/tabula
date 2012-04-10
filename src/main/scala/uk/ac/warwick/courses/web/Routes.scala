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
		
	object assignment {
		def apply(assignment:Assignment) = "/module/%s/%s/" format (encoded(assignment.module.code), encoded(assignment.id))
		def receipt(assignment:Assignment) = apply(assignment)
	}
	
	object admin {
		def department(department:Department) = "/admin/department/%s/" format (encoded(department.code))
		def module(module:Module) = department(module.department) + "#module-" + encoded(module.code)
		def modulePermissions(module:Module) = "/admin/module/%s/permissions" format (encoded(module.code))
		
		object assignment {
			def create(module:Module) = "/admin/module/%s/assignments/new" format (encoded(module.code))
			def edit(assignment:Assignment) = "/admin/module/%s/assignments/%s/edit" format (encoded(assignment.module.code), assignment.id)
			def delete(assignment:Assignment) = "/admin/module/%s/assignments/%s/delete" format (encoded(assignment.module.code), assignment.id)
			
			object submission {
				def apply(assignment:Assignment) = "/admin/module/%s/assignments/%s/submissions/list" format (encoded(assignment.module.code), assignment.id)
			}
			
			object feedback {
				def apply(assignment:Assignment) = "/admin/module/%s/assignments/%s/feedback/list" format (encoded(assignment.module.code), assignment.id)
			}
		}
	}
	
	object sysadmin {
		object events {
			def query = "/sysadmin/audit/search"
		}
	}
}

// Could do something like this to centralise all the @RequestMapping locations?
/*object Mappings {
	object admin {
		final val permissions = "/admin/module/{module}/permissions"
		object assignment {
			final val create = "/admin/module/{module}/assignments/new"
		}
	}
}*/