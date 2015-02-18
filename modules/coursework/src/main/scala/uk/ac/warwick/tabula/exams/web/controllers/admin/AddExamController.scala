package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.exams.commands.AddExamCommand
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/exam/new"))
class AddExamController extends BaseController {

	@ModelAttribute("command")
	def command(@PathVariable("module") module: Module) = AddExamCommand(mandatory(module))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm() = Mav("exams/admin/module/new")

}
