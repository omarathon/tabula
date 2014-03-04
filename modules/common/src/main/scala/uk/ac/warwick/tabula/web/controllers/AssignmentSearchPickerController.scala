package uk.ac.warwick.tabula.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.commands.{Appliable, Command, ReadOnly, Unaudited, CommandInternal}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AutowiringAssignmentServiceComponent, AssignmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.JSONView


@Controller
@RequestMapping(value = Array("/api/assignmentpicker/query"))
class AssignmentSearchPickerController extends BaseController {

	@ModelAttribute("command")
	def command = AssignmentSearchPickerCommand()

	@RequestMapping
	def query(@ModelAttribute("command")cmd: Appliable[Seq[Assignment]]) = {
		val assignments = cmd.apply()
		Mav(
			new JSONView(
				assignments.map(assignment => Map(
					"id" -> assignment.id,
					"name" -> assignment.name,
					"module" -> assignment.module.code,
					"department" -> assignment.module.department.name
				))
			)
		)
	}

}

class AssignmentSearchPickerCommand extends CommandInternal[Seq[Assignment]] {

	self: AssignmentServiceComponent =>

	var query: String = _

	def applyInternal() = {
		if (query.isEmpty) {
			Seq()
		} else {
			assignmentService.findAssignmentsByNameOrModule(query)
		}
	}

}

object AssignmentSearchPickerCommand {
	def apply() = new AssignmentSearchPickerCommand with Command[Seq[Assignment]] with AutowiringAssignmentServiceComponent
	with ReadOnly with Unaudited with Public
}
