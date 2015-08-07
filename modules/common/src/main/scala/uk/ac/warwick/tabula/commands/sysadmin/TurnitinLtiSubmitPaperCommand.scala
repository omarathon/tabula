package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions

import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.turnitinlti._

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.springframework.validation.Errors
import java.net.{MalformedURLException, URL}
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment}

object TurnitinLtiSubmitPaperCommand {
	def apply(user: CurrentUser) =
		new TurnitinLtiSubmitPaperCommandInternal(user)
			with TurnitinLtiSubmitPaperCommandPermissions
			with ComposableCommand[TurnitinLtiResponse]
			with ReadOnly with Unaudited
			with TurnitinLtiSubmitPaperCommandState
			with TurnitinLtiSubmitPaperValidation
			with AutowiringTurnitinLtiServiceComponent
}

class TurnitinLtiSubmitPaperCommandInternal(val user: CurrentUser) extends CommandInternal[TurnitinLtiResponse] {

	self: TurnitinLtiSubmitPaperCommandState with TurnitinLtiServiceComponent =>

	override def applyInternal() = transactional() {
		val userEmail = if (user.email == null || user.email.isEmpty) user.firstName + user.lastName + "@TurnitinLti.warwick.ac.uk" else user.email
		turnitinLtiService.submitPaper(assignment, paperUrl, userEmail, attachment, user.universityId, "SYSADMIN")
	}

}

trait TurnitinLtiSubmitPaperCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.GodMode)
	}
}

trait TurnitinLtiSubmitPaperValidation extends SelfValidating {
	self: TurnitinLtiSubmitPaperCommandState =>

	override def validate(errors: Errors) {
		if (assignment.turnitinId.isEmptyOrWhitespace) {
			errors.rejectValue("assignment", "assignment.turnitinid.empty")
		}
		if (paperUrl.isEmptyOrWhitespace) {
			errors.rejectValue("paperUrl", "turnitin.paperurl.empty")
		} else {
			try {
				val uri = new URL(paperUrl).toURI
				if (!uri.getHost.equals("files.warwick.ac.uk") && (!uri.getHost.equals("tabula.warwick.ac.uk"))){
					errors.rejectValue("paperUrl", "turnitin.paperurl.invalid")
				}
			} catch {
				case e: MalformedURLException => {
					errors.rejectValue("paperUrl", "turnitin.paperurl.invalid")
				}
			}
		}
	}
}

trait TurnitinLtiSubmitPaperCommandState {
	var paperUrl: String = _
	var assignment: Assignment = _
	var attachment: FileAttachment = _
}