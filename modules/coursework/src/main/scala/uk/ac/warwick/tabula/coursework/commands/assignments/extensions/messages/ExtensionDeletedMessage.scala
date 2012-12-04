package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment

class ExtensionDeletedMessage(assignment:Assignment, userId: String)
	extends ExtensionMessage(assignment:Assignment, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(getSubjectPrefix + "Extension revoked")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/revoke_manual_extension.ftl", Map(
			"originalAssignmentDate" -> dateFormatter.print(assignment.closeDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> recipient,
			"path" -> Routes.assignment.apply(assignment)
		)))
		baseMessage
	}

}