package uk.ac.warwick.courses.commands.assignments

import scala.reflect.BeanProperty
import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.data.model.Submission
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.util.mail.WarwickMailSender
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.courses.commands.Description
import javax.annotation.Resource
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.helpers.FreemarkerRendering
import freemarker.template.Configuration
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.helpers.StringUtils._
import org.joda.time.format.DateTimeFormatterBuilder
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.commands.ReadOnly

/**
 * Send an email confirming the receipt of a submission to the student
 * who submitted it.
 */
@Configurable
class SendSubmissionReceiptCommand (
		@BeanProperty var submission:Submission,
		@BeanProperty var user:CurrentUser
		) extends Command[Boolean] with ReadOnly with FreemarkerRendering {
	
	def this() = this(null,null)
	
	@BeanProperty var assignment:Assignment = Option(submission).map{ _.assignment }.orNull
	@BeanProperty var module:Module = Option(assignment).map{ _.module }.orNull
	
	@BeanProperty @Autowired implicit var freemarker:Configuration =_
	@Resource(name="studentMailSender") var studentMailSender:WarwickMailSender =_
	@Value("${mail.noreply.to}") var replyAddress:String = _
	@Value("${mail.exceptions.to}") var fromAddress:String = _
	@Value("${toplevel.url}") var topLevelUrl:String = _
	
	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")
	
	def apply = {
		if (user.email.hasText) {
			(studentMailSender send messageFor(user))
			true
		} else {
			false
		}
	}
	
	def messageFor(user:CurrentUser) = {
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
	    	"url" -> (topLevelUrl + Routes.assignment.receipt(assignment))
	    )))
		message
	}
	
	override def describe(d:Description) {
		d.assignment(assignment)
	}
	
}