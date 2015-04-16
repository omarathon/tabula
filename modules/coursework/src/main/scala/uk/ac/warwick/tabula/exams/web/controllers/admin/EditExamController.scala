package uk.ac.warwick.tabula.exams.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.exams.commands.{EditExamCommand, EditExamCommandState, ModifiesExamMembership, PopulateEditExamCommand}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}/edit"))
class EditExamController extends ExamsController {

	type EditExamCommand = Appliable[Exam] with EditExamCommandState with ModifiesExamMembership with PopulateEditExamCommand

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable("exam") exam : Exam) = EditExamCommand(mandatory(exam))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: EditExamCommand) = {
		cmd.populateGroups(cmd.exam)
		cmd.afterBind()

		Mav("exams/admin/edit",
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups,
			"department" -> cmd.module.adminDepartment,
			"markingWorkflows" -> cmd.module.adminDepartment.markingWorkflows.filter(_.validForExams)
		).crumbs(
				Breadcrumbs.Department(cmd.module.adminDepartment, cmd.academicYear),
				Breadcrumbs.Module(cmd.module, cmd.academicYear)
			)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: EditExamCommand,
		errors: Errors
	) = {
			cmd.afterBind()
			if (errors.hasErrors) {
				showForm(cmd)
			} else {
				cmd.apply()
				Redirect(Routes.admin.module(cmd.exam.module, cmd.exam.academicYear))
			}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}
}
