package uk.ac.warwick.courses.commands.assignments.extensions.messages

import org.springframework.beans.factory.annotation.{Value, Autowired, Configurable}
import reflect.BeanProperty
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.courses.web.views.FreemarkerRendering
import uk.ac.warwick.courses.data.model.{Module, Assignment}
import freemarker.template.Configuration
import javax.annotation.Resource
import uk.ac.warwick.util.mail.WarwickMailSender
import org.joda.time.format.DateTimeFormat
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.courses.helpers.StringUtils._
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.UserLookupService

/**
 * Send an email confirming the creation of a manual extension request to the student
 */
@Configurable
abstract class ExtensionMessage(@BeanProperty var extension: Extension, @BeanProperty var assignment: Assignment,
								@BeanProperty var uniId: String)
	extends Command[Boolean] with ReadOnly with FreemarkerRendering with Logging {

	def this(assignment:Assignment, uniId:String) = this(null, assignment, uniId)
	def this(extension:Extension, uniId:String) = this(extension, extension.assignment, uniId)
	def this() = this(null, null, null)

	@BeanProperty @Autowired var userLookup: UserLookupService = _
	@BeanProperty @Autowired implicit var freemarker: Configuration = _
	@Resource(name = "studentMailSender") var studentMailSender: WarwickMailSender = _

	@BeanProperty var module: Module = Option(assignment).map { _.module }.orNull

	// email constants
	@Value("${mail.noreply.to}") var replyAddress: String = _
	@Value("${mail.exceptions.to}") var fromAddress: String = _
	@Value("${toplevel.url}") var topLevelUrl: String = _

	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	def recipient = userLookup.getUserByWarwickUniId(uniId)

	def apply() = {
		if (recipient.getEmail.hasText) {
			val baseMessage = generateBaseMessage()
			val message = setMessageContent(baseMessage)
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

	// generates a message with common attributes pre-defined
	def generateBaseMessage():SimpleMailMessage = {
		val message = new SimpleMailMessage
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(recipient.getEmail)
		message
	}

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) : SimpleMailMessage

}
