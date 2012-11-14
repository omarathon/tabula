package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.data.model.forms.Extension

class ExtensionGrantedMessage(extension: Extension, userId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(module.code + ": Extension granted")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/new_manual_extension.ftl", Map(
			"extension" -> extension,
			"newExpirtyDate" -> dateFormatter.print(extension.getExpiryDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> recipient,
			"url" -> (topLevelUrl + Routes.assignment.apply(assignment)))))
		baseMessage
	}

}