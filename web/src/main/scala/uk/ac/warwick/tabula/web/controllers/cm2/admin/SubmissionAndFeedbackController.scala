package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.cm2.SubmissionAndFeedbackInfoFilters
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

//FIXME: Summary page for cm2 needs fixing. Not implemented yet

abstract class AbstractSubmissionAndFeedbackController extends CourseworkController
	with AcademicYearScopedController
	with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	validatesSelf[SelfValidating]


	@ModelAttribute("submissionAndFeedbackCommand")
	def command(@PathVariable assignment: Assignment): SubmissionAndFeedbackCommand.CommandType =
		SubmissionAndFeedbackCommand(assignment)


	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

	lazy val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))


}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class SubmissionAndFeedbackController extends AbstractSubmissionAndFeedbackController
	with AcademicYearScopedController
	with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {


	@RequestMapping(Array("/list"))
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

	@RequestMapping(Array("/summary"))
	def summary(
		@Valid @ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType,
		errors: Errors, @PathVariable assignment: Assignment,
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {

		if (!features.assignmentProgressTable) Redirect(Routes.admin.assignment.submissionsandfeedback.table(assignment))
		else {
			if (errors.hasErrors) {
				Mav("cm2/admin/assignments/submissionsandfeedback/progress",
					"module" -> assignment.module,
					"department" -> assignment.module.adminDepartment,
					"academicYear" -> academicYear
				)

			} else {

				val results = command.apply()
				Mav("cm2/admin/assignments/submissionsandfeedback/progress",
					"module" -> assignment.module,
					"department" -> assignment.module.adminDepartment,
					"academicYear" -> academicYear,
					"results" -> results
				)
			}
		}
	}

}


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/table"))
class SubmissionAndFeedbackTableController extends AbstractSubmissionAndFeedbackController
	with AcademicYearScopedController
	with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@RequestMapping
	def table(@ModelAttribute("submissionAndFeedbackCommand") command: SubmissionAndFeedbackCommand.CommandType, @PathVariable assignment: Assignment): Mav = {
		val results = command.apply()
		if (ajax) {
			Mav("cm2/admin/assignments/submissionsandfeedback/table_results",
				"results" -> results,
				"department" -> assignment.module.adminDepartment,
				"academicYear" -> academicYear,
				"module" -> assignment.module
			).noLayout()
		} else {
			Mav("cm2/admin/assignments/submissionsandfeedback/list",
				"results" -> results,
				"department" -> assignment.module.adminDepartment,
				"allSubmissionStatesFilters" -> SubmissionAndFeedbackInfoFilters.SubmissionStates.allSubmissionStates.filter(_.apply(assignment)),
				"allPlagiarismFilters" -> SubmissionAndFeedbackInfoFilters.PlagiarismStatuses.allPlagiarismStatuses.filter(_.apply(assignment)),
				"allStatusFilters" -> SubmissionAndFeedbackInfoFilters.Statuses.allStatuses.filter(_.apply(assignment)),
				"academicYear" -> academicYear
			)
		}
	}

}