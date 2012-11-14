package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import freemarker.template.Configuration
import javax.annotation.Resource
import javax.annotation.Resource
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.coursework.commands.Command
import uk.ac.warwick.tabula.coursework.commands.Description
import uk.ac.warwick.tabula.coursework.commands.SelfValidating
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.coursework.services.AssignmentService
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.views.FreemarkerRendering
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire

class PublishFeedbackCommand extends Command[Unit] with FreemarkerRendering with SelfValidating {

	var studentMailSender = Wire[WarwickMailSender]("studentMailSender")
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]
	implicit var freemarker = Wire.auto[Configuration]

	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")
	var topLevelUrl = Wire.property("${toplevel.url}")
	
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
	@BeanProperty var confirm: Boolean = false

	case class MissingUser(val universityId: String)
	case class BadEmail(val user: User, val exception: Exception = null)

	var missingUsers: JList[MissingUser] = ArrayList()
	var badEmails: JList[BadEmail] = ArrayList()

	// validation done even when showing initial form.
	def prevalidate(errors: Errors) {
		if (assignment.isClosed()) {
			errors.rejectValue("assignment", "feedback.publish.notclosed")
		} else if (assignment.feedbacks.isEmpty()) {
			errors.rejectValue("assignment", "feedback.publish.nofeedback")
		}
	}

	def validate(implicit errors: Errors) {
		prevalidate(errors)
		if (!confirm) {
			rejectValue("confirm", "feedback.publish.confirm")
		}
	}

	def work {
		transactional() {
			val users = assignmentService.getUsersForFeedback(assignment)
			for ((studentId, user) <- users) {
				val feedbacks = assignment.feedbacks.find { _.universityId == studentId }
				for (feedback <- feedbacks)
					feedback.released = true
			}
			for (info <- users) email(info)
		}
	}

	private def email(info: Pair[String, User]) {
		val (id, user) = info
		if (user.isFoundUser()) {
			val email = user.getEmail
			if (StringUtils.hasText(email)) {
				val message = messageFor(user)
				try {
					studentMailSender.send(message)
				} catch {
					case e: MailException => badEmails add BadEmail(user, exception = e)
				}
			} else {
				badEmails add BadEmail(user)
			}
		} else {
			missingUsers add MissingUser(id)
		}
	}

	private def messageFor(user: User): SimpleMailMessage = {
		val message = new SimpleMailMessage
		val moduleCode = assignment.module.code.toUpperCase
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(user.getEmail)
		// TODO configurable subject
		message.setSubject(moduleCode + ": Your coursework feedback is ready")
		// TODO configurable body
		message.setText(messageTextFor(user))

		return message
	}

	def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(assignment.feedbacks.map { _.universityId })

	def messageTextFor(user: User) =
		renderToString("/WEB-INF/freemarker/emails/feedbackready.ftl", Map(
			"name" -> user.getFirstName,
			"assignmentName" -> assignment.name,
			"moduleCode" -> assignment.module.code.toUpperCase,
			"moduleName" -> assignment.module.name,
			"url" -> (topLevelUrl + Routes.assignment.receipt(assignment))))

}