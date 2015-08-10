package uk.ac.warwick.tabula.commands.coursework.turnitinlti

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.helpers.StringUtils._

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.Logging
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Module, FileAttachment, Assignment}

object TurnitinLtiViewReportCommand {
	def apply(module: Module, assignment: Assignment, attachment: FileAttachment, user: CurrentUser) =
		new TurnitinLtiViewReportCommandInternal(module, assignment, attachment, user)
			with TurnitinLtiViewReportCommandPermissions
			with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			// TODO notifications
			// with CompletesNotifications[Mav]
			with TurnitinLtiViewReportCommandState
			with TurnitinLtiViewReportValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class TurnitinLtiViewReportCommandInternal(
		val module: Module, val assignment: Assignment, val attachment: FileAttachment, val user: CurrentUser)
	extends CommandInternal[TurnitinLtiResponse]{

	self: TurnitinLtiViewReportCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal() = transactional() {
		turnitinLtiService.getOriginalityReportUrl(assignment, attachment, user)
	}

}

trait TurnitinLtiViewReportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: TurnitinLtiViewReportCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Submission.ViewPlagiarismStatus, assignment)
	}
}

trait TurnitinLtiViewReportValidation extends SelfValidating {
	self: TurnitinLtiViewReportCommandState =>

	override def validate(errors: Errors) {
		if (attachment.originalityReport == null) {
			errors.rejectValue("fileAttachment", "fileattachment.originalityReport.empty")
		} else if (attachment.originalityReport.turnitinId.isEmptyOrWhitespace) {
			errors.rejectValue("fileAttachment", "fileattachment.originalityReport.invalid")
		}
	}
}

trait TurnitinLtiViewReportCommandState {
	def assignment: Assignment
	def module: Module
	def attachment: FileAttachment
}