package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.Routes.admin.module
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.{Student, SubmissionAndFeedbackResults, SubmissionAndFeedbackCommand}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Submission, StudentMember, Assignment, Module}
import uk.ac.warwick.tabula.commands.{SelfValidating, ReadOnly, Unaudited, Command}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import org.joda.time.DateTime

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback"))
class OnlineFeedbackController extends CourseworkController{

	@ModelAttribute("onlineFeedbackCommand")
	def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new OnlineFeedbackCommand(module, assignment)

	@RequestMapping(Array("/online"))
	def showTable(@Valid command: OnlineFeedbackCommand, errors: Errors) {
		val (assignment, module) = (command.assignment, command.module)

		Mav("/admin/assignments/feedback/online_framework.ftl")
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}

class OnlineFeedbackCommand(val module: Module, val assignment: Assignment)
	extends Command[Seq[RandomData]] with Unaudited with ReadOnly with SelfValidating {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Submission.Read, assignment)

	def applyInternal() = {
		???
	}

	def validate(errors: Errors) {
		true
	}
}

case class RandomData(
	val student: StudentMember,
	val submission: Option[Submission],
	val hasFeedback: Boolean,
	val hasPublishedFeedback: Boolean
)