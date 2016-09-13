package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.DownloadFeedbackAsPdfCommand
import uk.ac.warwick.tabula.commands.profiles.PhotosWarwickMemberPhotoUrlGeneratorComponent
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(value = Array("/coursework/module/{module}/{assignment}/{student}/feedback.pdf"))
class OldDownloadFeedbackAsPdfController extends OldCourseworkController {

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
			DownloadFeedbackAsPdfCommand.feedbackDownloadTemple,
			Map(
				"feedback" -> command.apply(),
				"studentId" -> student.universityId
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent with PhotosWarwickMemberPhotoUrlGeneratorComponent
	}

}
