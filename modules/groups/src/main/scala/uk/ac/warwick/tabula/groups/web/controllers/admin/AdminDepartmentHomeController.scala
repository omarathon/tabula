package uk.ac.warwick.tabula.groups.web.controllers.admin

import scala.collection.JavaConverters._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.AdminDepartmentHomeCommand


@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends GroupsController {

	hideDeletedItems

	@ModelAttribute def command(@PathVariable("department") dept: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(dept, user)

	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set

	@RequestMapping(method=Array(GET, HEAD))
	def adminDepartment(cmd: AdminDepartmentHomeCommand) = {
		val modules = cmd.apply()
		// mapping from module ID to the available group sets.
		val setMapping: Map[String, Seq[SmallGroupSet]] = modules.map {
			module => module.id -> module.groupSets.asScala
		}.toMap

		Mav("admin/department",
			"department" -> cmd.department,
			"modules" -> modules,
			"setMapping" -> setMapping )
	}
}
