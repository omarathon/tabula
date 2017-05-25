package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.cm2.{SubmissionAndFeedbackInfoFilter, SubmissionAndFeedbackInfoFilters}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class SubmissionAndFeedbackController extends CourseworkController with AutowiringFeaturesComponent {

	@ModelAttribute("submissionAndFeedbackCommand")
	def command(@PathVariable assignment: Assignment): SubmissionAndFeedbackCommand.CommandType =
		SubmissionAndFeedbackCommand(assignment)

	@RequestMapping(Array("", "/list"))
	def list(@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		assignment.module.adminDepartment.assignmentInfoView match {
			case Assignment.Settings.InfoViewType.Summary =>
				Redirect(Routes.admin.assignment.submissionsandfeedback.summary(assignment))
			case Assignment.Settings.InfoViewType.Table =>
				Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
			case _ => // default
				if (features.assignmentProgressTableByDefault)
					Redirect(Routes.admin.assignment.submissionsandfeedback.summary(assignment))
				else
					Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		}
	}

	@ModelAttribute("allSubmissionStatesFilters")
	def allSubmissionStatesFilters(@PathVariable assignment: Assignment): Seq[SubmissionAndFeedbackInfoFilter] =
		SubmissionAndFeedbackInfoFilters.SubmissionStates.allSubmissionStates.filter(_.apply(assignment))

	@ModelAttribute("allPlagiarismFilters")
	def allPlagiarismFilters(@PathVariable assignment: Assignment): Seq[SubmissionAndFeedbackInfoFilter] =
		SubmissionAndFeedbackInfoFilters.PlagiarismStatuses.allPlagiarismStatuses.filter(_.apply(assignment))

	@ModelAttribute("allStatusFilters")
	def allStatusFilters(@PathVariable assignment: Assignment): Seq[SubmissionAndFeedbackInfoFilter] =
		SubmissionAndFeedbackInfoFilters.Statuses.allStatuses.filter(_.apply(assignment))

	@ModelAttribute("department") def department(@PathVariable assignment: Assignment): Department = assignment.module.adminDepartment
	@ModelAttribute("module") def module(@PathVariable assignment: Assignment): Module = assignment.module
	@ModelAttribute("academicYear") def academicYear(@PathVariable assignment: Assignment): AcademicYear = assignment.academicYear

	@RequestMapping(Array("/summary"))
	def summary(@ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable assignment: Assignment): Mav =
		if (!features.assignmentProgressTable) Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		else if (ajax) Mav("cm2/admin/assignments/submissionsandfeedback/summary_results", "results" -> command.apply()).noLayout()
		else Mav("cm2/admin/assignments/submissionsandfeedback/summary", "results" -> command.apply())
			.crumbsList(Breadcrumbs.assignment(assignment, active = true))

	@RequestMapping(Array("/table"))
	def table(@ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable assignment: Assignment): Mav =
		if (ajax) Mav("cm2/admin/assignments/submissionsandfeedback/table_results", "results" -> command.apply()).noLayout()
		else Mav("cm2/admin/assignments/submissionsandfeedback/table", "results" -> command.apply())
			.crumbsList(Breadcrumbs.assignment(assignment, active = true))

}