package uk.ac.warwick.tabula.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, Command, ReadOnly, Unaudited, CommandInternal}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent, AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.JSONView


case class ModulePickerResult(module: Module, hasSmallGroups: Boolean)

@Controller
@RequestMapping(value = Array("/api/modulepicker/query"))
class ModulePickerController extends BaseController {

	@ModelAttribute("command")
	def command = ModulePickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command")cmd: Appliable[Seq[ModulePickerResult]]) = {
		val results = cmd.apply()
		Mav(
			new JSONView(
				results.map(result => Map(
					"id" -> result.module.id,
					"code" -> result.module.code,
					"name" -> result.module.name,
					"department" -> result.module.department.name,
					"hasSmallGroups" -> result.hasSmallGroups)
				)
			)
		)
	}

}

class ModulePickerCommand extends CommandInternal[Seq[ModulePickerResult]] {

	self: ModuleAndDepartmentServiceComponent with SmallGroupServiceComponent =>

	var query: String = _

	def applyInternal() = {
		if (query.isEmpty) {
			Seq()
		} else {
			val modules: Seq[Module] = moduleAndDepartmentService.findModulesNamedLike(query)
			modules.map (module => ModulePickerResult(module, smallGroupService.hasSmallGroups(module)))
		}
	}

}

object ModulePickerCommand {
	def apply() = new ModulePickerCommand with Command[Seq[ModulePickerResult]] with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringSmallGroupServiceComponent
	with ReadOnly with Unaudited with Public
}
