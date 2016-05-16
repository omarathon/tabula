package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.commands.exams._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/delete"))
class DeleteExamController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable exam : Exam) = new DeleteExamCommand(mandatory(exam))

	@RequestMapping(method = Array(GET))
	def showForm(@ModelAttribute("command") cmd: DeleteExamCommand) = {
		render(cmd)
	}

	private def render(cmd: DeleteExamCommand) = {
		Mav("exams/exams/admin/delete").crumbs(
			Breadcrumbs.Exams.Home,
			Breadcrumbs.Exams.Department(cmd.exam.module.adminDepartment, cmd.exam.academicYear),
			Breadcrumbs.Exams.Module(cmd.exam.module, cmd.exam.academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: DeleteExamCommand,
		errors: Errors
	) = {
		if (errors.hasErrors) {
			render(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.Exams.admin.module(cmd.exam.module, cmd.exam.academicYear))
		}
	}
}
