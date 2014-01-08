package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentCourseDetails, FileAttachment, Assignment, Module, Member}
import uk.ac.warwick.tabula.data.Transactions._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser, DateFormats}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.permissions._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications.{ExtensionRequestModifiedNotification, ExtensionRequestCreatedNotification}
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{RelationshipService}
import uk.ac.warwick.tabula.validators.WithinYears



class ExtensionRequestCommand(val module: Module, val assignment:Assignment, val submitter: CurrentUser)
	extends Command[Extension]  with Notifies[Extension, Option[Extension]] with Daoisms with BindListener with SelfValidating {

	var relationshipService = Wire.auto[RelationshipService]

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Extension.MakeRequest, assignment)

	val basicInfo = Map("moduleManagers" -> module.managers.users)
	val studentRelationships = relationshipService.allStudentRelationshipTypes

	val extraInfo = basicInfo ++ (submitter.profile.flatMap { _.mostSignificantCourseDetails.map { scd =>

		val relationships = studentRelationships.map(x => (x.description, relationshipService.findCurrentRelationships(x,scd.sprCode))).toMap

		//Pick only the parts of scd required since passing the whole object fails due to the session not being available to load lazy objects
		Map(
			"relationships" -> relationships.filter({case (relationshipType,relations) => relations.length != 0}),
			"scdCourse" -> scd.course,
			"scdRoute" -> scd.route,
			"scdAward" -> scd.award
		)
	}}).getOrElse(Map())

	var reason:String =_
	
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@WithinYears(maxFuture = 3)
	var requestedExpiryDate:DateTime =_
	
	var file:UploadedFile = new UploadedFile
	var attachedFiles:JSet[FileAttachment] =_
	var readGuidelines:JBoolean =_
	// true if this command is modifying an existing extension. False otherwise
	var modified:JBoolean = false

	def validate(errors:Errors){
		if(!submitter.apparentUser.getUserId.hasText){
			errors.reject("extension.noValidUserId")
		}

		if (!readGuidelines){
			errors.rejectValue("readGuidelines","extension.readGuidelines.mustConfirmRead" )
		}
		if (requestedExpiryDate == null){
			errors.rejectValue("requestedExpiryDate", "extension.requestedExpiryDate.provideExpiry")
		} else if(requestedExpiryDate.isBefore(assignment.closeDate)){
			errors.rejectValue("requestedExpiryDate", "extension.requestedExpiryDate.beforeAssignmentExpiry")
		}
		if (!reason.hasText){
			errors.rejectValue("reason", "extension.reason.provideReasons")
		}
	}

	def presetValues(extension:Extension){
		reason = extension.reason
		requestedExpiryDate = extension.requestedExpiryDate
		attachedFiles = extension.attachments
		modified = true
	}

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

		if (extension.attachments != null){// delete attachments that have been removed
			if(attachedFiles == null){
				attachedFiles = Set[FileAttachment]()
			}
			(extension.attachments -- attachedFiles).foreach(session.delete(_))
		}
		if (!file.attached.isEmpty) {
			for (attachment <- file.attached) {
				extension.addAttachment(attachment)
			}
		}

		session.saveOrUpdate(extension)
		extension
	}

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
	}
	
	override def describeResult(d: Description, extension: Extension) {
		d.assignment(assignment)
		 .property("extension" -> extension.id)
		 .fileAttachments(extension.attachments.toSeq)
	}

	def emit(extension: Extension) = {
		if (modified){
			Seq(new ExtensionRequestModifiedNotification(extension, submitter.apparentUser, extraInfo) with FreemarkerTextRenderer)
		} else {
			Seq(new ExtensionRequestCreatedNotification(extension, submitter.apparentUser, extraInfo) with FreemarkerTextRenderer)
		}
	}
}
