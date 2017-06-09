package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.cm2.feedback._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/online"))
class OnlineFeedbackListController extends CourseworkController {

	type Command = Appliable[Seq[EnhancedFeedback]] with OnlineFeedbackListState

	@ModelAttribute
	def command(@PathVariable assignment: Assignment): Command = OnlineFeedbackListCommand(mandatory(assignment))

	@RequestMapping
	def showTable(@PathVariable assignment: Assignment, @ModelAttribute command: Command, @RequestParam(value="usercode", required=false) usercode: String): Mav = {
		Mav("cm2/admin/assignments/feedback/online_feedback_list",
			"department" -> assignment.module.adminDepartment,
			"studentToOpen" -> usercode,
			"enhancedFeedbacks" -> command.apply()
		)
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/online/{student}"))
class OnlineFeedbackController extends CourseworkController {

	validatesSelf[SelfValidating]

	type Command = Appliable[Feedback] with OnlineFeedbackState

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable student: User, submitter: CurrentUser) = OnlineFeedbackCommand(
		mandatory(assignment),
		mandatory(student),
		submitter,
		GenerateGradesFromMarkCommand(mandatory(assignment))
	)

	@RequestMapping
	def showForm(@ModelAttribute("command") command: Command, errors: Errors): Mav = {
		Mav("cm2/admin/assignments/feedback/online_feedback",
			"isGradeValidation" -> command.assignment.module.adminDepartment.assignmentGradeValidation,
			"command" -> command
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: Command, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}
