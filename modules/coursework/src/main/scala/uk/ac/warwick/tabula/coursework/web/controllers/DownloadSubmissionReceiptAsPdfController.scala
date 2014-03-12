package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Submission, Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.{PermissionDeniedException, CurrentUser}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.services.{SubmissionServiceComponent, AutowiringSubmissionServiceComponent}

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/submission-receipt.pdf"))
class DownloadSubmissionReceiptAsPdfController extends CourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	@ModelAttribute
	def command(
		@PathVariable("module") module: Module,
		@PathVariable("assignment") assignment: Assignment,
		user: CurrentUser
	): DownloadSubmissionReceiptAsPdfCommand = DownloadSubmissionReceiptAsPdfCommand(module, assignment, user)

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

	def apply(module: Module, assignment: Assignment, user: CurrentUser) =
		new DownloadSubmissionReceiptAsPdfCommandInternal(module, assignment, user)
			with AutowiringSubmissionServiceComponent
			with DownloadSubmissionReceiptAsPdfPermissions
			with ComposableCommand[Submission]
			with ReadOnly with Unaudited
}

class DownloadSubmissionReceiptAsPdfCommandInternal(val module: Module, val assignment: Assignment, val user: CurrentUser)
	extends CommandInternal[Submission]
		with DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	override def applyInternal() = submissionOption.getOrElse(throw new IllegalStateException)
}

trait DownloadSubmissionReceiptAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadSubmissionReceiptAsPdfState with SubmissionServiceComponent =>

	def permissionsCheck(p: PermissionsChecking) {
		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!user.loggedIn) {
			throw new PermissionDeniedException(user, DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, assignment)
		}

		notDeleted(mandatory(assignment))
		mustBeLinked(assignment, mandatory(module))

		val submission = mandatory(submissionOption)

		mustBeLinked(submission, assignment)
		p.PermissionCheck(DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, submission)
	}
}

trait DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	def module: Module
	def assignment: Assignment
	def user: CurrentUser

	lazy val submissionOption = submissionService.getSubmissionByUniId(assignment, user.universityId).filter(_.submitted)
}
