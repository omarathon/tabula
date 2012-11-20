package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.util.mail.WarwickMailSender
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.commands.Description
import javax.annotation.Resource
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import freemarker.template.Configuration
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire

/**
 * Send an email confirming the receipt of a submission to the student
 * who submitted it.
 */
class SendSubmissionReceiptCommand(
	@BeanProperty var submission: Submission,
	@BeanProperty var user: CurrentUser) extends Command[Boolean] with ReadOnly with FreemarkerRendering {

	def this() = this(null, null)

	@BeanProperty var assignment: Assignment = Option(submission).map { _.assignment }.orNull
	@BeanProperty var module: Module = Option(assignment).map { _.module }.orNull

	implicit var freemarker = Wire.auto[Configuration]
	var studentMailSender = Wire[WarwickMailSender]("studentMailSender")
	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")
	var topLevelUrl = Wire.property("${toplevel.url}")

	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	def applyInternal() = {
		if (user.email.hasText) {
			(studentMailSender send messageFor(user))
			true
		} else {
			false
		}
	}

	def messageFor(user: CurrentUser) = {
		val message = new SimpleMailMessage
		val moduleCode = module.code.toUpperCase
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(user.email)
		message.setSubject(moduleCode + ": Submission receipt")
		message.setText(renderToString("/WEB-INF/freemarker/emails/submissionreceipt.ftl", Map(
			"submission" -> submission,
			"submissionDate" -> dateFormatter.print(submission.submittedDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> user,
			"url" -> (topLevelUrl + Routes.assignment.receipt(assignment)))))
		message
	}

	override def describe(d: Description) {
		d.assignment(assignment)
	}

}