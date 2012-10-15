package uk.ac.warwick.courses.commands.assignments.extensions.messages

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.data.model.Assignment

@Configurable
class ExtensionDeletedMessage(assignment:Assignment, userId: String)
	extends ExtensionMessage(assignment:Assignment, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(module.code + ": Extension revoked")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/revoke_manual_extension.ftl", Map(
			"originalAssignmentDate" -> dateFormatter.print(assignment.closeDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> recipient,
			"url" -> (topLevelUrl + Routes.assignment.apply(assignment)))))
		baseMessage
	}

}