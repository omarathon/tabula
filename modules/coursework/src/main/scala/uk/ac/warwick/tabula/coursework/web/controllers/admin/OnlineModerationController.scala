package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.commands.feedback.OnlineModerationCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Member}
import uk.ac.warwick.tabula.CurrentUser
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.MarkingState.{Rejected, MarkingCompleted}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/online/moderation/{student}"))
class OnlineModerationController extends CourseworkController {

	validatesSelf[OnlineModerationCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: User, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineModerationCommand(module, assignment, student, currentUser)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineModerationCommand, errors: Errors): Mav = {

		val (isCompleted, completedDate, firstMarkerFeedback) = command.markerFeedback match {
			case Some(mf) => (
				mf.state == MarkingCompleted || mf.state == Rejected ,
				mf.uploadedDate,
				mf.feedback.firstMarkerFeedback
			)
			case None => (false, null, None)
		}

		Mav("admin/assignments/feedback/marker_moderation" ,
			"command" -> command,
			"isCompleted" -> isCompleted,
			"completedDate" -> completedDate,
			"firstMarkerFeedback" -> firstMarkerFeedback
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: OnlineModerationCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			val markerFeedbackState: String = command.markerFeedback.map(_.state.toString).getOrElse("")
			Mav("ajax_success", "data" -> markerFeedbackState).noLayout()
		}
	}

}
