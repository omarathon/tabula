package uk.ac.warwick.tabula.exams.web.controllers.admin


import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating, UpstreamGroup, UpstreamGroupPropertyEditor}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{AddExamCommand, ExamState, ModifiesExamMembership}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/new"))
class AddExamController extends ExamsController {

	type AddExamCommand = Appliable[Exam] with ExamState with ModifiesExamMembership

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		 @PathVariable("module") module: Module,
		 @PathVariable("academicYear") academicYear : AcademicYear) = AddExamCommand(mandatory(module), mandatory(academicYear))

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(@ModelAttribute("command") cmd: AddExamCommand, @PathVariable("module") module: Module) = {
		cmd.afterBind()
		Mav("exams/admin/new",
			"availableUpstreamGroups" -> cmd.availableUpstreamGroups,
			"linkedUpstreamAssessmentGroups" -> cmd.linkedUpstreamAssessmentGroups,
			"assessmentGroups" -> cmd.assessmentGroups,
			"department" -> module.adminDepartment,
			"markingWorkflows" -> module.adminDepartment.markingWorkflows.filter(_.validForExams))
			"assessmentGroups" -> cmd.assessmentGroups
		).crumbs(
				Breadcrumbs.Department(cmd.module.adminDepartment, cmd.academicYear),
				Breadcrumbs.Module(cmd.module, cmd.academicYear)
		)
	}


	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: AddExamCommand,
						 @PathVariable("module") module: Module,
						 errors: Errors) = {
		cmd.afterBind()
		if (errors.hasErrors) {
			showForm(cmd, module)
		} else {
			cmd.apply()
			Redirect(Routes.admin.module(cmd.module, cmd.academicYear))
		}
	}

	@InitBinder
	def upstreamGroupBinder(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[UpstreamGroup], new UpstreamGroupPropertyEditor)
	}

}
