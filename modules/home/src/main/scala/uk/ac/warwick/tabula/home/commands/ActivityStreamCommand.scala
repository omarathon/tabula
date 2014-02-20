package uk.ac.warwick.tabula.home.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.{PubliclyVisiblePermissions, PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities
import uk.ac.warwick.tabula.services._


object ActivityStreamCommand {
	def apply(request: ActivityStreamRequest) =
		new ActivityStreamCommandInternal(request)
			with ComposableCommand[PagedActivities]
			//with ActivityStreamCommandPermissions
			//with ActivityStreamCommandDescription
			with Unaudited
			with PubliclyVisiblePermissions
			with ActivityStreamCommandValidation
}

abstract class ActivityStreamCommandInternal(
		val request: ActivityStreamRequest
	) extends CommandInternal[PagedActivities]
		with ActivityStreamCommandState
		with ReadOnly
		with NotificationServiceComponent {

	def applyInternal() = {
		val results = notificationService.stream(request)
		new PagedActivities(
			results.items,
			results.last,
			results.token,
			results.total
		)
	}
}

trait ActivityStreamCommandValidation extends SelfValidating {
		self: ActivityStreamCommandState =>
	def validate(errors: Errors) {

	}
}

trait ActivityStreamCommandState {
	def request: ActivityStreamRequest
}

trait ActivityStreamCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
		self: ActivityStreamCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {

	}
}
/*

trait ActivityStreamCommandDescription extends Describable[PagedActivities] {
	self: ActivityStreamCommandState =>
	def describe(d: Description) {

	}
}*/
