package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.data.model.{ UserGroup, Module, Assignment }
import uk.ac.warwick.courses.commands.{ Description, Command }
import reflect.BeanProperty
import uk.ac.warwick.courses.CurrentUser
import org.springframework.beans.factory.annotation.{ Value, Autowired, Configurable }
import uk.ac.warwick.userlookup.{ User, UserLookup }
import collection.JavaConversions._
import uk.ac.warwick.courses.web.views.FreemarkerRendering
import freemarker.template.Configuration
import javax.annotation.Resource
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.courses.web.Routes
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.spring.Wire

/**
 * Sends a message to one or more admins to let them know that the current
 * user thinks they should have access to an assignment.
 */
class RequestAssignmentAccessCommand(user: CurrentUser) extends Command[Unit] with FreemarkerRendering {

	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _

	var userLookup = Wire.auto[UserLookupService]
	implicit var freemarker = Wire.auto[Configuration]
	var mailSender = Wire[WarwickMailSender]("mailSender")
	var topLevelUrl = Wire.property("${toplevel.url}")
	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")

	override def work() {
		val admins =
			if (!module.participants.isEmpty) module.participants
			else module.department.owners

		val adminUsers = userLookup.getUsersByUserIds(seqAsJavaList(admins.members))
		val manageAssignmentUrl = topLevelUrl + Routes.admin.assignment.edit(assignment)

		for ((usercode, admin) <- adminUsers if admin.isFoundUser) {
			val messageText = renderToString("/WEB-INF/freemarker/emails/requestassignmentaccess.ftl", Map(
				"assignment" -> assignment,
				"student" -> user,
				"admin" -> admin,
				"url" -> manageAssignmentUrl))
			val message = new SimpleMailMessage
			val moduleCode = module.code.toUpperCase
			message.setFrom(fromAddress)
			message.setReplyTo(replyAddress)
			message.setTo(admin.getEmail)
			message.setSubject(moduleCode + ": Access request")
			message.setText(messageText)

			mailSender.send(message)
		}

	}

	// describe the thing that's happening.
	override def describe(d: Description) {
		d.assignment(assignment)
	}
}
