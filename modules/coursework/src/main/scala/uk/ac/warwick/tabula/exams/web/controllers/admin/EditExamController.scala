package uk.ac.warwick.tabula.exams.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.exams.commands.{EditExamCommand, EditExamCommandState}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}/edit"))
class EditExamController extends ExamsController {

	type EditExamCommand = Appliable[Exam] with EditExamCommandState

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable("exam") exam : Exam) = EditExamCommand(mandatory(exam))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: EditExamCommand) = Mav("exams/admin/edit")

	@RequestMapping(method = Array(POST))
	def submit(
			@Valid @ModelAttribute("command") cmd: EditExamCommand,
			errors: Errors
	) = {
			if (errors.hasErrors) {
				showForm(cmd)
			} else {
				cmd.apply()
				Redirect(Routes.admin.module(cmd.exam.module, cmd.exam.academicYear))
			}
	}
}
