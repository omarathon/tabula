package uk.ac.warwick.tabula.coursework.web.controllers

import uk.ac.warwick.tabula._
import actions._
import data.model._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype._
import collection.JavaConversions._
import uk.ac.warwick.tabula.commands.ViewViewableCommand

class ViewModuleCommand(module: Module) extends ViewViewableCommand(module)

@Controller
@RequestMapping(Array("/module/{module}/"))
class ModuleController extends CourseworkController {

	hideDeletedItems
	
	@ModelAttribute def command(@PathVariable module: Module) = new ViewModuleCommand(module)

	@RequestMapping
	def viewModule(@ModelAttribute cmd: ViewModuleCommand) = {
		val module = cmd.apply()
		
		Mav("submit/module",
			"module" -> module,
			"assignments" -> module.assignments
				.filterNot { _.deleted }
				.sortBy { _.closeDate }
				.reverse)
	}

}
