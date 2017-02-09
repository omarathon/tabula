package uk.ac.warwick.tabula.commands.coursework.assignments.extensions

import uk.ac.warwick.tabula.commands.{Description, Describable, CommandInternal, SelfValidating}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.tabula.{DateFormats, CurrentUser}
import uk.ac.warwick.tabula.data.model.{FileAttachment, Module, Assignment}
import uk.ac.warwick.tabula.validators.WithinYears
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.data.Daoisms


abstract class ModifyExtensionCommand(val mod: Module, val ass: Assignment, uniId: String, val sub: CurrentUser, val act: String = "")
		extends CommandInternal[Extension] with ModifyExtensionCommandState {
	self: ExtensionPersistenceComponent with UserLookupComponent =>

	universityId = uniId
	module = mod
	assignment = ass
	submitter = sub
	action = act

	def copyTo(extension: Extension): Unit = {
		extension.userId = userLookup.getUserByWarwickUniId(universityId).getUserId
		extension.assignment = assignment
		extension.expiryDate = expiryDate
		extension.rawState_=(state)
		extension.reviewedOn = DateTime.now

		action match {
			case ApprovalAction | UpdateApprovalAction => extension.approve(reviewerComments)
			case RejectionAction => extension.reject(reviewerComments)
			case RevocationAction => extension.rawState_=(ExtensionState.Revoked)
			case _ =>
		}
	}

	def copyFrom(extension: Extension): Unit = {
		expiryDate = extension.expiryDate.orNull
		state = extension.state
		reviewerComments = extension.reviewerComments
	}

}


trait ModifyExtensionCommandValidation extends SelfValidating {
	self: ModifyExtensionCommandState with UserLookupComponent =>
	def validate(errors: Errors) {

		val userId = userLookup.getUserByWarwickUniId(universityId).getUserId
		if(userId == null) {
			errors.rejectValue("universityId", "extension.universityId.noValidUserId")
		}

		if(expiryDate == null) {
			if (action == ApprovalAction || action == UpdateApprovalAction) {
				errors.rejectValue("expiryDate", "extension.requestedExpiryDate.provideExpiry")
			}
		} else if(expiryDate.isBefore(assignment.closeDate)) {
			errors.rejectValue("expiryDate", "extension.expiryDate.beforeAssignmentExpiry")
		}
	}
}


trait ModifyExtensionCommandState {

	var isNew: Boolean = _

	var universityId: String =_
	var assignment: Assignment =_
	var module: Module =_
	var submitter: CurrentUser =_

	@WithinYears(maxFuture = 3) @DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var expiryDate: DateTime =_
	var reviewerComments: String =_
	var state: ExtensionState = ExtensionState.Unreviewed
	var action: String =_
	var extension: Extension =_

	final val ApprovalAction = "Grant"
	final val RejectionAction = "Reject"
	final val RevocationAction = "Revoke"
	final val UpdateApprovalAction = "Update"
}


trait ModifyExtensionCommandDescription extends Describable[Extension] {
	self: ModifyExtensionCommandState =>

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(module)
		d.studentIds(Seq(universityId))
	}
}


/**
 * This could be a separate service, but it's so noddy it's not (yet) worth it
 */
trait HibernateExtensionPersistenceComponent extends ExtensionPersistenceComponent with Daoisms {
	def delete(attachment: FileAttachment): Unit = {
		attachment.extension.removeAttachment(attachment)
		session.delete(attachment)
	}
	def delete(extension: Extension): Unit = session.delete(extension)
	def save(extension: Extension): Unit = session.saveOrUpdate(extension)
}

trait ExtensionPersistenceComponent {
	def delete(attachment: FileAttachment)
	def delete(extension: Extension)
	def save(extension: Extension)
}