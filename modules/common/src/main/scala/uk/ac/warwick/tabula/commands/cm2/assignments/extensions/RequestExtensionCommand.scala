package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import uk.ac.warwick.tabula.data.model.notifications.coursework._

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.Transactions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments.extensions.ModifyExtensionCommandState
import uk.ac.warwick.tabula.data.model.forms.ExtensionState.MoreInformationReceived
import uk.ac.warwick.tabula.events.NotificationHandling

import scala.collection.mutable

object RequestExtensionCommand {

	def apply (assignment: Assignment, submitter: CurrentUser, action: String) = {
		new RequestExtensionCommandInternal(assignment, submitter) with
			ComposableCommand[Extension] with
			AutowiringRelationshipServiceComponent with
			RequestExtensionCommandDescription with
			RequestExtensionCommandPermission with
			RequestExtensionCommandValidation with
			RequestExtensionCommandNotification with
			HibernateExtensionPersistenceComponent
	}
}

class RequestExtensionCommandInternal(val assignment:Assignment, val submitter: CurrentUser)
	extends CommandInternal[Extension]
	with RequestExtensionCommandState
	with BindListener {

	self: RelationshipServiceComponent with ExtensionPersistenceComponent =>

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	override def applyInternal() = transactional() {
		val universityId = submitter.apparentUser.getWarwickId
		val extension = assignment.findExtension(universityId).getOrElse({
			val newExtension = new Extension(universityId)
			newExtension.userId = submitter.apparentUser.getUserId
			newExtension
		})
		extension.assignment = assignment
		extension.requestedExpiryDate = requestedExpiryDate
		extension.reason = reason
		extension.requestedOn = DateTime.now
		extension.disabilityAdjustment = Option(Boolean.unbox(disabilityAdjustment)).getOrElse(false)

		if (extension.attachments != null) {
			// delete attachments that have been removed
			val matchingAttachments: mutable.Set[FileAttachment] = extension.attachments -- attachedFiles
			matchingAttachments.foreach(delete)
		}

		for (attachment <- file.attached) {
			extension.addAttachment(attachment)
		}

		if (extension.state == ExtensionState.MoreInformationRequired) {
			extension._state = MoreInformationReceived
		}

		save(extension)
		extension
	}
}


trait RequestExtensionCommandValidation extends SelfValidating {
	self: RequestExtensionCommandState =>

	def validate(errors: Errors) {
		if(!submitter.apparentUser.getUserId.hasText) {
			errors.reject("extension.noValidUserId")
		}
		if (!readGuidelines) {
			errors.rejectValue("readGuidelines","extension.readGuidelines.mustConfirmRead" )
		}
		if (requestedExpiryDate == null) {
			errors.rejectValue("requestedExpiryDate", "extension.requestedExpiryDate.provideExpiry")
		} else if (requestedExpiryDate.isBefore(assignment.closeDate)) {
			errors.rejectValue("requestedExpiryDate", "extension.requestedExpiryDate.beforeAssignmentExpiry")
		}
		if (!reason.hasText) {
			errors.rejectValue("reason", "extension.reason.provideReasons")
		}
		val hasValidExtension = assignment
			.findExtension(submitter.apparentUser.getWarwickId)
			.filter(_.approved)
			.flatMap(_.expiryDate)
			.exists(_.isAfterNow)

		if (!hasValidExtension && !assignment.newExtensionsCanBeRequested) {
			errors.reject("assignment.extensionRequests.disallowed")
		}

		if (assignment.extensionAttachmentMandatory && attachedFiles.isEmpty && file.attached.isEmpty) {
			errors.rejectValue("file", "extension.file.required")
		}
	}
}


trait RequestExtensionCommandPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: RequestExtensionCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Extension.MakeRequest, assignment)
	}
}


trait RequestExtensionCommandDescription extends Describable[Extension] {
	self: RequestExtensionCommandState =>

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
	}

	override def describeResult(d: Description, extension: Extension) {
		d.assignment(assignment)
			.property("extension" -> extension.id)
			.fileAttachments(extension.attachments.toSeq)
	}
}


trait RequestExtensionCommandNotification extends Notifies[Extension, Option[Extension]] {
	self: RequestExtensionCommandState with RelationshipServiceComponent =>

	val basicInfo = Map("moduleManagers" -> assignment.module.managers.users)
	val studentRelationships = relationshipService.allStudentRelationshipTypes

	val relationshipAndCourseInfo = for {
		student <- submitter.profile.collect { case student: StudentMember => student }
		scd <- student.mostSignificantCourseDetails
		relationships = studentRelationships.map(relType =>
			(relType.description, relationshipService.findCurrentRelationships(relType, scd.student))
		).toMap
	} yield Map(
		"relationships" -> relationships.filter({case (relationshipType,relations) => relations.nonEmpty}),
		"scdCourse" -> scd.course,
		"scdRoute" -> scd.currentRoute,
		"scdAward" -> scd.award
	)

	val extraInfo = basicInfo ++ relationshipAndCourseInfo.getOrElse(Map())

	def emit(extension: Extension) = {
		val agent = submitter.apparentUser
		val assignment = extension.assignment
		val baseNotification = if(extension.moreInfoReceived) {
			new ExtensionInfoReceivedNotification
		} else if (modified) {
			new ExtensionRequestModifiedNotification
		} else {
			new ExtensionRequestCreatedNotification
		}
		Seq(Notification.init(baseNotification, agent, Seq(extension), assignment))
	}
}

trait RequestExtensionCommandState {
	val assignment:Assignment
	val submitter: CurrentUser

	def presetValues(extension: Extension) {
		reason = extension.reason
		requestedExpiryDate = extension.requestedExpiryDate.orNull
		attachedFiles = extension.attachments
		modified = true
		disabilityAdjustment = extension.disabilityAdjustment
	}

	var reason: String =_

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@WithinYears(maxFuture = 3)
	var requestedExpiryDate: DateTime =_

	var file: UploadedFile = new UploadedFile
	var attachedFiles: JSet[FileAttachment] = JSet()
	@beans.BeanProperty var readGuidelines: JBoolean = false
	// true if this command is modifying an existing extension. False otherwise
	var modified: JBoolean = false
	var disabilityAdjustment: JBoolean = false
}
