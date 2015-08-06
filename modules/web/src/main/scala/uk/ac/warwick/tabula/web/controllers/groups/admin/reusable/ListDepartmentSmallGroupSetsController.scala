package uk.ac.warwick.tabula.web.controllers.groups.admin.reusable

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.commands.groups.admin.reusable.{ListDepartmentSmallGroupSetsCommandState, ListDepartmentSmallGroupSetsCommand}
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

@Controller
@RequestMapping(value=Array("/groups/admin/department/{department}/groups/reusable"))
class ListDepartmentSmallGroupSetsController extends GroupsController {

	hideDeletedItems

	type ListDepartmentSmallGroupSetsCommand = Appliable[Seq[DepartmentSmallGroupSet]] with ListDepartmentSmallGroupSetsCommandState

	@ModelAttribute("command") def command(@PathVariable("department") department: Department) =
		ListDepartmentSmallGroupSetsCommand(department)

	@RequestMapping def list(@ModelAttribute("command") command: ListDepartmentSmallGroupSetsCommand) = {
		Mav("groups/admin/groups/reusable/list",
			"sets" -> command.apply()
		).crumbs(Breadcrumbs.Department(command.department))
	}

}
