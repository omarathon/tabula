package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages
import scala.beans.BeanProperty

import org.joda.time.format.DateTimeFormat
import org.springframework.mail.javamail.MimeMessageHelper

import freemarker.template.Configuration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender
import language.implicitConversions

/**
 * Send an email confirming the creation of a manual extension request to the student
 */
abstract class ExtensionMessage(var extension: Extension, var assignment: Assignment,
								var userId: String)
	extends Command[Boolean] with ReadOnly with FreemarkerRendering with Logging with UnicodeEmails {

	def this(assignment:Assignment, uniId:String) = this(null, assignment, uniId)
	def this(extension:Extension, uniId:String) = this(extension, extension.assignment, uniId)
	def this() = this(null, null, null)

	var userLookup = Wire.auto[UserLookupService]
	implicit var freemarker= Wire.auto[Configuration]
	var studentMailSender = Wire[WarwickMailSender]("studentMailSender")

	var module: Module = Option(assignment).map { _.module }.orNull

	// email constants
	var replyAddress: String = Wire.property("${mail.noreply.to}")
	var fromAddress: String = Wire.property("${mail.exceptions.to}")

	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	def recipient = userLookup.getUserByUserId(userId)

	def applyInternal() = {
		if (recipient.getEmail.hasText) {
			val message = createMessage(studentMailSender) { message =>
				generateBaseMessage(message)
				setMessageContent(message)
			}

			studentMailSender.send(message)
			true
		}
		else {
			logger.error("Unable to send extension message to "+recipient.getUserId+". No email address is specified.")
			false
		}
	}

	override def describe(d: Description) {
		d.assignment(assignment)
		d.module(module)
	}
	
	// text to put at the start of all email subjects.
	protected def getSubjectPrefix = module.code.toUpperCase + ": "

	// generates a message with common attributes pre-defined
	def generateBaseMessage(message: MimeMessageHelper): Unit = {
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(recipient.getEmail)
	}

	// applied to a base message to set a context specific subject and body
	def setMessageContent(message: MimeMessageHelper): Unit

}
