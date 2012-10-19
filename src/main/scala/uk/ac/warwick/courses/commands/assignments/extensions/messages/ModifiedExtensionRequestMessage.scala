package uk.ac.warwick.courses.commands.assignments.extensions.messages

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.data.model.forms.Extension

@Configurable
class ModifiedExtensionRequestMessage(extension: Extension, userId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(module.code + ": Extension request approved")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/extension_request_approved.ftl", Map(
			"extension" -> extension,
			"newExpirtyDate" -> dateFormatter.print(extension.getExpiryDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> recipient,
			"url" -> (topLevelUrl + Routes.assignment.apply(assignment)))))
		baseMessage
	}

}