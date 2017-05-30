package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.controllers.cm2.AbstractGenerateGradeFromMarkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/generate-grade"))
class GenerateAssignmentGradeFromMarkController extends AbstractGenerateGradeFromMarkController[Assignment] {

	@ModelAttribute("command")
	def command(module: Module, @PathVariable assignment: Assignment) =
		GenerateGradesFromMarkCommand(mandatory(assignment).module, mandatory(assignment))

}
