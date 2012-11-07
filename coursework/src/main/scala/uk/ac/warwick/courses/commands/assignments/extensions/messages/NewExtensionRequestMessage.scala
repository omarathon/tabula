package uk.ac.warwick.courses.commands.assignments.extensions.messages

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.data.model.forms.Extension

class NewExtensionRequestMessage(extension: Extension, userId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(module.code + ": New extension request made")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/new_extension_request.ftl", Map(
			"requestedExpirtyDate" -> dateFormatter.print(extension.requestedExpiryDate),
			"reasonForRequest" -> extension.reason,
			"attachments" -> extension.attachments,
			"assignment" -> assignment,
			"student" -> userLookup.getUserByUserId(extension.getUserId),
			"module" -> module,
			"user" -> recipient,
			"url" -> (topLevelUrl + Routes.admin.assignment.extension.review(assignment, extension.universityId)))))
		baseMessage
	}

}