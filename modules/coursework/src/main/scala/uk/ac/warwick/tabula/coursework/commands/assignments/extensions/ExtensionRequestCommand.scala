package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{FileAttachment, Assignment}
import uk.ac.warwick.tabula.data.Transactions._
import reflect.BeanProperty
import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.DateFormats
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.BindingResult


class ExtensionRequestCommand(val module: Module, val assignment:Assignment, val submitter: CurrentUser)
	extends Command[Extension] with Daoisms with BindListener with SelfValidating {
	
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Extension.MakeRequest, assignment)

	@BeanProperty var reason:String =_
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var requestedExpiryDate:DateTime =_
	@BeanProperty var file:UploadedFile = new UploadedFile
	@BeanProperty var attachedFiles:JSet[FileAttachment] =_
	@BeanProperty var readGuidelines:JBoolean =_
	// true if this command is modifying an existing extension. False otherwise
	@BeanProperty var modified:JBoolean = false

	def validate(errors:Errors){
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
}
