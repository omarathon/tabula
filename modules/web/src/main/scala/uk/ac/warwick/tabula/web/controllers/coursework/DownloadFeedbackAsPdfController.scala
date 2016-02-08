package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.web.views.PDFView
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.commands._
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.data.model._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/coursework/module/{module}/{assignment}/{student}/feedback.pdf"))
class DownloadFeedbackAsPdfController extends CourseworkController {

	type DownloadFeedbackAsPdfCommand = Appliable[Feedback]
	var feedbackService = Wire[FeedbackService]

	@ModelAttribute def command(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable student: Member): DownloadFeedbackAsPdfCommand = {

		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!user.loggedIn) {
			throw new PermissionDeniedException(user, Permissions.AssignmentFeedback.Read, assignment)
		}

		DownloadFeedbackAsPdfCommand(module, assignment, mandatory(feedbackService.getAssignmentFeedbackByUniId(assignment, student.universityId)), student)
	}

	@RequestMapping
	def viewAsPdf(command: DownloadFeedbackAsPdfCommand, @PathVariable student: Member) = {
		new PDFView(
			"feedback.pdf",
			"/WEB-INF/freemarker/admin/assignments/markerfeedback/feedback-download.ftl",
			Map("feedback" -> command.apply(), "user"-> student.asSsoUser)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent
	}

}

object DownloadFeedbackAsPdfCommand {
	def apply(module: Module, assignment: Assignment, feedback: Feedback, student: Member) =
		new DownloadFeedbackAsPdfCommandInternal(module, assignment, feedback, student)
			with ComposableCommand[Feedback]
			with DownloadFeedbackAsPdfPermissions
			with DownloadFeedbackAsPdfAudit
}

class DownloadFeedbackAsPdfCommandInternal(val module: Module, val assignment: Assignment, val feedback: Feedback, val student: Member)
	extends CommandInternal[Feedback] with DownloadFeedbackAsPdfState {
	override def applyInternal() = feedback
}

trait DownloadFeedbackAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadFeedbackAsPdfState =>

	def permissionsCheck(p: PermissionsChecking) {
		notDeleted(assignment)
		mustBeLinked(assignment, module)

		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.AssignmentFeedback.Read, student),
				CheckablePermission(Permissions.AssignmentFeedback.Read, feedback))
		)
	}
}

trait DownloadFeedbackAsPdfAudit extends Describable[Feedback] {
	self: DownloadFeedbackAsPdfState =>

	def describe(d: Description) {
		d.feedback(feedback)
	}
}

trait DownloadFeedbackAsPdfState {
	val module: Module
	val assignment: Assignment
	val feedback: Feedback
	val student: Member
}
