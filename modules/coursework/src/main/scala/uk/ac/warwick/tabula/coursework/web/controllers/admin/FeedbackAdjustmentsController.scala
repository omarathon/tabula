package uk.ac.warwick.tabula.coursework.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Feedback, Assignment}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/adjustments"))
class FeedbackAdjustmentsListController extends CourseworkController {

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable assignment: Assignment) =
		FeedbackAdjustmentListCommand(assignment)

	@RequestMapping(method=Array(GET))
	def list(@PathVariable assignment: Assignment,
					 @ModelAttribute("listCommand") listCommand: Appliable[Seq[StudentInfo]]
	) = {
		val studentInfo = listCommand.apply()
		Mav("admin/assignments/feedback/adjustments_list",
			"studentInfo" -> studentInfo,
			"assignment" -> assignment,
			"isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation
		).crumbs(
			Breadcrumbs.Department(assignment.module.adminDepartment),
			Breadcrumbs.Module(assignment.module)
		)
	}
}

object FeedbackAdjustmentsController {
	final val LATE_PENALTY_PER_DAY = 5
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/adjustments/{student}"))
class FeedbackAdjustmentsController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def formCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable student: User, submitter: CurrentUser) =
		FeedbackAdjustmentCommand(mandatory(assignment), student, submitter, GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment)))

	@RequestMapping(method=Array(GET))
	def showForm(@ModelAttribute("command") command: Appliable[Feedback] with FeedbackAdjustmentCommandState,
							 errors: Errors,
							 @PathVariable assignment: Assignment,
							 @PathVariable student: User) = {

		val daysLate = command.submission.map(_.workingDaysLate)
		val marksSubtracted = daysLate.map(FeedbackAdjustmentsController.LATE_PENALTY_PER_DAY * _)
		val proposedAdjustment = for(am <- command.feedback.actualMark; ms <- marksSubtracted)
			yield Math.max(0, (am - ms))


		Mav("admin/assignments/feedback/adjustments", Map(
			"daysLate" -> daysLate,
			"marksSubtracted" -> marksSubtracted,
			"proposedAdjustment" -> proposedAdjustment,
			"latePenalty" -> FeedbackAdjustmentsController.LATE_PENALTY_PER_DAY,
			"isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation
		)).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: Appliable[Feedback] with FeedbackAdjustmentCommandState,
						 errors: Errors,
						 @PathVariable assignment: Assignment,
						 @PathVariable student: User)  = {

		if (errors.hasErrors) {
			showForm(command, errors, assignment, student)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}