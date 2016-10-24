package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears


object ModifyExtensionCommand {
	def apply(extension: Extension) = new ModifyExtensionCommandInternal(extension)
		with ComposableCommand[Extension]
		with ModifyExtensionValidation
		with ModifyExtensionPermissions
		with ModifyExtensionDescription
		with HibernateExtensionPersistenceComponent
}

class ModifyExtensionCommandInternal(val extension: Extension) extends CommandInternal[Extension]
	with ModifyExtensionState with ModifyExtensionValidation  with TaskBenchmarking {

	this: ExtensionPersistenceComponent  =>

	expiryDate = extension.expiryDate.orNull
	reviewerComments = extension.reviewerComments
	state = extension.state

	def copyTo(extension: Extension) = {
		extension.expiryDate = expiryDate
		extension.updateState(state, reviewerComments)
	}

	def applyInternal() = transactional() {
		copyTo(extension)
		save(extension)
		extension
	}

}

trait ModifyExtensionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ModifyExtensionState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Extension.Update, extension)
	}
}

trait ModifyExtensionState {
	val extension: Extension
	var submitter: CurrentUser = _

	@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var expiryDate: DateTime = _
	var reviewerComments: String = _
	var state: ExtensionState = _
}

trait ModifyExtensionValidation extends SelfValidating {
	self: ModifyExtensionState =>
	def validate(errors: Errors) {
		if(expiryDate == null) {
			if (state == ExtensionState.Approved) {
				errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
			}
		} else if(expiryDate.isBefore(extension.assignment.closeDate)) {
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}

		if(!Option(reviewerComments).exists(_.nonEmpty) && state == ExtensionState.MoreInformationRequired) {
			errors.rejectValue("reviewerComments", "extension.reviewerComments.provideReasons")
		}
	}
}

trait ModifyExtensionDescription extends Describable[Extension] {
	self: ModifyExtensionState =>

	def describe(d: Description) {
		d.assignment(extension.assignment)
		d.module(extension.assignment.module)
		d.studentIds(Seq(extension.universityId))
	}
}