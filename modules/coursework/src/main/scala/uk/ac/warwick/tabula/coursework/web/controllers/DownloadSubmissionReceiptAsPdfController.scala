package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Member, Submission, Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
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
	): DownloadSubmissionReceiptAsPdfCommand = DownloadSubmissionReceiptAsPdfCommand(module, assignment, user, currentMember)

	@RequestMapping
	def viewAsPdf(command: DownloadSubmissionReceiptAsPdfCommand, user: CurrentUser) = {
		new PDFView(
			"submission-receipt.pdf",
			"/WEB-INF/freemarker/submit/submission-receipt.ftl",
			Map(
				"submission" -> command.apply()
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent
	}

}

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/{studentMember}/submission-receipt.pdf"))
class DownloadSubmissionReceiptForStudentAsPdfController extends CourseworkController {

	hideDeletedItems

	type DownloadSubmissionReceiptAsPdfCommand = Appliable[Submission] with DownloadSubmissionReceiptAsPdfState

	@ModelAttribute
	def command(
		 @PathVariable("module") module: Module,
		 @PathVariable("assignment") assignment: Assignment,
		 @PathVariable("studentMember") studentMember: Member,
		 user: CurrentUser
	 ): DownloadSubmissionReceiptAsPdfCommand = DownloadSubmissionReceiptAsPdfCommand(module, assignment, user, studentMember)

	@RequestMapping
	def viewAsPdf(command: DownloadSubmissionReceiptAsPdfCommand, user: CurrentUser) = {
		new PDFView(
			"submission-receipt.pdf",
			"/WEB-INF/freemarker/submit/submission-receipt.ftl",
			Map(
				"submission" -> command.apply()
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent
	}

}

object DownloadSubmissionReceiptAsPdfCommand {
	val RequiredPermission = Permissions.Submission.Read

	def apply(module: Module, assignment: Assignment, user: CurrentUser, studentMember: Member) =
		new DownloadSubmissionReceiptAsPdfCommandInternal(module, assignment, user, studentMember)
			with AutowiringSubmissionServiceComponent
			with DownloadSubmissionReceiptAsPdfPermissions
			with ComposableCommand[Submission]
			with ReadOnly with Unaudited
}

class DownloadSubmissionReceiptAsPdfCommandInternal(val module: Module, val assignment: Assignment, val viewer: CurrentUser, val student: Member)
	extends CommandInternal[Submission]
		with DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	override def applyInternal() = submissionOption.getOrElse(throw new IllegalStateException)
}

trait DownloadSubmissionReceiptAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadSubmissionReceiptAsPdfState with SubmissionServiceComponent =>

	def permissionsCheck(p: PermissionsChecking) {
		// We send a permission denied explicitly (this would normally be a 404 for feedback not found) because PDF handling is silly in Chrome et al
		if (!viewer.loggedIn) {
			throw new PermissionDeniedException(viewer, DownloadSubmissionReceiptAsPdfCommand.RequiredPermission, assignment)
		}

		notDeleted(mandatory(assignment))
		mustBeLinked(assignment, mandatory(module))

		val submission = mandatory(submissionOption)

		mustBeLinked(submission, assignment)

		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Submission.Read, submission),
				CheckablePermission(Permissions.Submission.Read, student))
		)
	}
}

trait DownloadSubmissionReceiptAsPdfState {
	self: SubmissionServiceComponent =>

	def module: Module
	def assignment: Assignment
	def viewer: CurrentUser
	def student: Member

	lazy val submissionOption = submissionService.getSubmissionByUniId(assignment, student.asSsoUser.getWarwickId).filter(_.submitted)
}
