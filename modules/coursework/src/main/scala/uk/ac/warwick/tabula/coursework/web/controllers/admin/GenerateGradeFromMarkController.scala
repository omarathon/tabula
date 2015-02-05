package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.feedback.GenerateGradeFromMarkCommand
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/generate-grade"))
class GenerateGradeFromMarkController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		GenerateGradeFromMarkCommand(mandatory(module), mandatory(assignment))

	@RequestMapping(method= Array(POST))
	def post(@ModelAttribute("command") cmd: Appliable[Map[String, Option[String]]]) = {
		val result = cmd.apply()

		Mav(new JSONView(
			result.map{case(uniID, gradeOption) => uniID -> gradeOption.getOrElse("")}
		))
	}

}
