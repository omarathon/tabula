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
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment}
import uk.ac.warwick.tabula.web.Mav

object TurnitinLtiViewReportCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiViewReportCommandInternal(user)
				with TurnitinLtiViewReportCommandPermissions
				with ComposableCommand[Mav]
			with ReadOnly with Unaudited
			with TurnitinLtiViewReportCommandState
			with TurnitinLtiViewReportValidation
			with AutowiringTurnitinLtiServiceComponent
			with Logging
}

class TurnitinLtiViewReportCommandInternal(val user: CurrentUser) extends CommandInternal[Mav]{

	self: TurnitinLtiViewReportCommandState with TurnitinLtiServiceComponent with Logging =>

	override def applyInternal() = transactional() {
		val response = turnitinLtiService.getOriginalityReportUrl(assignment, attachment, user)
		if (response.redirectUrl.isDefined) Mav(s"redirect:${response.redirectUrl.get}")
		else Mav("admin/assignments/turnitin/report_error", "problem" -> "no-object")
	}

}

trait TurnitinLtiViewReportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ManageEmergencyMessage)
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
	var assignment: Assignment = _
	var attachment: FileAttachment = _
}