package uk.ac.warwick.tabula.exams.web.controllers.admin


import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{ModifiesExamMembership, AddExamCommand, AddExamCommandState}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/new"))
class AddExamController extends ExamsController {

	type AddExamCommand = Appliable[Exam] with AddExamCommandState with ModifiesExamMembership

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		 @PathVariable("module") module: Module,
		 @PathVariable("academicYear") academicYear : AcademicYear) = AddExamCommand(mandatory(module), mandatory(academicYear))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: AddExamCommand) = Mav("exams/admin/new")


	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddExamCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module, cmd.examAcademicYear))
		}
	}
}
