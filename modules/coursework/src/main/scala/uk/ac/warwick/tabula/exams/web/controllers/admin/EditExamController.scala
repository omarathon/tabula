package uk.ac.warwick.tabula.exams.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{EditExamCommand, EditExamCommandState}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exam/{exam}/edit"))
class EditExamController extends ExamsController {

	type EditExamCommand = Appliable[Exam] with EditExamCommandState with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable("exam") exam : Exam) = EditExamCommand(mandatory(exam))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(
			@ModelAttribute("command") cmd: EditExamCommand,
			@ModelAttribute("module") module: Module,
			@ModelAttribute("academicYear") academicYear: AcademicYear
	) = {
			cmd.populate()
			Mav("exams/admin/edit",
				"module" -> module,
				"academicYear" -> academicYear
			)
	}

	@RequestMapping(method = Array(POST))
	def submit(
			@Valid @ModelAttribute("command") cmd: EditExamCommand,
			@ModelAttribute("module") module: Module,
			@ModelAttribute("academicYear") academicYear: AcademicYear,
			errors: Errors
	) = {
			if (errors.hasErrors) {
				showForm(cmd, module, academicYear)
			} else {
				cmd.apply()
				Redirect(Routes.home)
			}
	}
}
