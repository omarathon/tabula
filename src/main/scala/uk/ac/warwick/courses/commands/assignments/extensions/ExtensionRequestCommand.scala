package uk.ac.warwick.courses.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.courses.commands.{UploadedFile, Description, Command}
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.data.model.{FileAttachment, Assignment}
import uk.ac.warwick.courses.data.Transactions._
import reflect.BeanProperty
import org.joda.time.DateTime
import uk.ac.warwick.courses.{DateFormats, CurrentUser}
import org.springframework.validation.Errors
import uk.ac.warwick.courses.helpers.StringUtils._
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.beans.factory.annotation.Configurable

class ExtensionRequestCommand(val assignment:Assignment, val submitter: CurrentUser)
	extends Command[Extension] with Daoisms {

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

	def onBind() = transactional() {
		file.onBind
	}

	override def work() = transactional() {

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
