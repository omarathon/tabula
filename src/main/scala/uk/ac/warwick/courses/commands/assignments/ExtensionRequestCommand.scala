package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.{SelfValidating, Description, Command}
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import reflect.BeanProperty
import org.joda.time.DateTime
import uk.ac.warwick.courses.{DateFormats, CurrentUser}
import org.springframework.validation.Errors
import uk.ac.warwick.courses.helpers.StringUtils._
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.beans.factory.annotation.Configurable

@Configurable
class ExtensionRequestCommand(val assignment:Assignment, val submitter: CurrentUser)
	extends Command[Extension] with SelfValidating with Daoisms {

	@BeanProperty var reason:String =_
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty var requestedExpiryDate:DateTime =_

	@BeanProperty var readGuidelines:JBoolean =_

	def validate(implicit errors:Errors){
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

	@Transactional
	override def apply() = {

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
		session.saveOrUpdate(extension)
		extension
	}

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
		//d.studentIds(Seq(submitter.apparentUser.getWarwickId))
		//d.properties("requestedExpiryDate" -> requestedExpiryDate)
	}
}
