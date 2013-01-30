package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import freemarker.template.Configuration
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.permissions._

/**
 * Send an email confirming the receipt of a submission to the student
 * who submitted it.
 */
class SendSubmissionReceiptCommand(val module: Module, val assignment: Assignment, val submission: Submission, val user: CurrentUser) extends Command[Boolean] with ReadOnly with FreemarkerRendering with UnicodeEmails {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.SendReceipt(), mandatory(submission))

	implicit var freemarker = Wire.auto[Configuration]
	var studentMailSender = Wire[WarwickMailSender]("studentMailSender")
	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")

	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	def applyInternal() = {
		if (user.email.hasText) {
			(studentMailSender send messageFor(user))
			true
		} else {
			false
		}
	}

	def messageFor(user: CurrentUser) = createMessage(studentMailSender) { message =>
		val moduleCode = module.code.toUpperCase
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(user.email)
		message.setSubject(encodeSubject(moduleCode + ": Submission receipt"))
		message.setText(renderToString("/WEB-INF/freemarker/emails/submissionreceipt.ftl", Map(
			"submission" -> submission,
			"submissionDate" -> dateFormatter.print(submission.submittedDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> user,
			"path" -> Routes.assignment.receipt(assignment)
		)))
	}

	override def describe(d: Description) {
		d.assignment(assignment)
	}

}