package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.commands.{Appliable, Description, Describable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{PermissionDeniedException, CurrentUser}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SubmissionService

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/submission-receipt.pdf"))
class DownloadSubmissionReceiptAsPdfController extends CourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	var submissionService = Wire[SubmissionService]

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser): DownloadSubmissionReceiptAsPdfCommand = {
		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!user.loggedIn) {
			throw new PermissionDeniedException(user, DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, assignment)
		}

		DownloadSubmissionReceiptAsPdfCommand(module, assignment, mandatory(submissionService.getSubmissionByUniId(assignment, user.universityId).filter { _.submitted }))
	}

	@RequestMapping
	def viewAsPdf(command: DownloadSubmissionReceiptAsPdfCommand, user: CurrentUser) = {
		new PDFView(
			"submission-receipt.pdf",
			"/WEB-INF/freemarker/submit/submission-receipt.ftl",
			Map(
				"submission" -> command.apply(),
				"user" -> user
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent
	}

}

object DownloadSubmissionReceiptAsPdfCommand {
	val RequiredPermission = Permissions.Submission.Read

	def apply(module: Module, assignment: Assignment, submission: Submission) =
		new DownloadSubmissionReceiptAsPdfCommandInternal(module, assignment, submission)
			with ComposableCommand[Submission]
			with DownloadSubmissionReceiptAsPdfPermissions
			with DownloadSubmissionReceiptAsPdfAudit
}

class DownloadSubmissionReceiptAsPdfCommandInternal(val module: Module, val assignment: Assignment, val submission: Submission) extends CommandInternal[Submission] with DownloadSubmissionReceiptAsPdfState {
	override def applyInternal() = submission
}

trait DownloadSubmissionReceiptAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadSubmissionReceiptAsPdfState =>

	def permissionsCheck(p: PermissionsChecking) {
		notDeleted(mandatory(assignment))
		mustBeLinked(assignment, mandatory(module))
		mustBeLinked(mandatory(submission), assignment)

		p.PermissionCheck(DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, submission)
	}
}

trait DownloadSubmissionReceiptAsPdfAudit extends Describable[Submission] {
	self: DownloadSubmissionReceiptAsPdfState =>

	def describe(d: Description) {
		d.submission(submission)
	}
}

trait DownloadSubmissionReceiptAsPdfState {
	val module: Module
	val assignment: Assignment
	val submission: Submission
}
