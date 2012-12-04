package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages

import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.forms.Extension

class ExtensionChangedMessage(extension: Extension, userId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(getSubjectPrefix() + "Extension details have been changed")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/modified_manual_extension.ftl", Map(
			"extension" -> extension,
			"newExpiryDate" -> dateFormatter.print(extension.expiryDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> recipient,
			"path" -> Routes.assignment.apply(assignment)
		)))
		baseMessage
	}

}
