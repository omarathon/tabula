package uk.ac.warwick.tabula.groups.web

import java.net.URLEncoder
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroupSet, SmallGroupSet}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/groups"
	def home = context + "/"

	object tutor {
		def mygroups = context + "/tutor"
	}

	object admin {
		def apply(department: Department) = context + "/admin/department/%s" format (encoded(department.code))

		def release(department: Department) = apply(department) + "/groups/release"
		def selfsignup(department: Department, action: String) = apply(department) + "/groups/selfsignup/" + encoded(action)

		object module {
			def apply(module: Module) = admin(module.department) + "#module-" + encoded(module.code)
		}

		def allocate(set: SmallGroupSet) = context + "/admin/module/%s/groups/%s/allocate" format (encoded(set.module.code), encoded(set.id))

		object reusable {
			def apply(department: Department) = context + "/admin/department/%s/groups/reusable" format (encoded(department.code))
			def create(department: Department) = context + "/admin/department/%s/groups/reusable/new" format (encoded(department.code))
			def createAddStudents(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/new/%s/students" format (encoded(set.department.code), encoded(set.id))
			def createAddGroups(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/new/%s/groups" format (encoded(set.department.code), encoded(set.id))
			def createAllocate(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/new/%s/allocate" format (encoded(set.department.code), encoded(set.id))
			def edit(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/edit/%s" format (encoded(set.department.code), encoded(set.id))
			def editAddStudents(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/edit/%s/students" format (encoded(set.department.code), encoded(set.id))
			def editAddGroups(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/edit/%s/groups" format (encoded(set.department.code), encoded(set.id))
			def editAllocate(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/edit/%s/allocate" format (encoded(set.department.code), encoded(set.id))
			def delete(set: DepartmentSmallGroupSet) = context + "/admin/department/%s/groups/reusable/delete/%s" format (encoded(set.department.code), encoded(set.id))
		}
	}
}
