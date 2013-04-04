package uk.ac.warwick.tabula.coursework.commands.assignments
import scala.collection.JavaConversions._
import scala.beans.BeanProperty

import freemarker.template.Configuration
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{ Description, Command }
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.{ Module, Assignment }
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.helpers.StringUtils._
import language.implicitConversions

/**
 * Sends a message to one or more admins to let them know that the current
 * user thinks they should have access to an assignment.
 */
class RequestAssignmentAccessCommand(user: CurrentUser) extends Command[Unit] with FreemarkerRendering with UnicodeEmails with Public {

	var module: Module = _
	var assignment: Assignment = _

	var userLookup = Wire.auto[UserLookupService]
	implicit var freemarker = Wire.auto[Configuration]
	var mailSender = Wire[WarwickMailSender]("mailSender")
	var fromAddress = Wire.property("${mail.exceptions.to}")

	override def applyInternal() {
		val admins = module.department.owners

		val adminUsers = userLookup.getUsersByUserIds(seqAsJavaList(admins.members))
		val manageAssignmentUrl = Routes.admin.assignment.edit(assignment)

		for ((usercode, admin) <- adminUsers if admin.isFoundUser && admin.getEmail.hasText) {
			val messageText = renderToString("/WEB-INF/freemarker/emails/requestassignmentaccess.ftl", Map(
				"assignment" -> assignment,
				"student" -> user,
				"admin" -> admin,
				"path" -> manageAssignmentUrl))
			val message = createMessage(mailSender) { message => 
				val moduleCode = module.code.toUpperCase
				message.setFrom(fromAddress)
				message.setReplyTo(user.email)
				message.setTo(admin.getEmail)
				message.setSubject(encodeSubject(moduleCode + ": Access request"))
				message.setText(messageText)
			}

			mailSender.send(message)
		}

	}

	// describe the thing that's happening.
	override def describe(d: Description) {
		d.assignment(assignment)
	}
}
