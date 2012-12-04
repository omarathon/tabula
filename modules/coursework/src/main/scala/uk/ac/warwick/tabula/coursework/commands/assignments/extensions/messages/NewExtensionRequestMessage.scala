package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages

import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.forms.Extension

class NewExtensionRequestMessage(extension: Extension, userId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	def setMessageContent(baseMessage: SimpleMailMessage) = {
		baseMessage.setSubject(getSubjectPrefix() + "New extension request made")
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/new_extension_request.ftl", Map(
			"requestedExpiryDate" -> dateFormatter.print(extension.requestedExpiryDate),
			"reasonForRequest" -> extension.reason,
			"attachments" -> extension.attachments,
			"assignment" -> assignment,
			"student" -> userLookup.getUserByUserId(extension.getUserId),
			"module" -> module,
			"user" -> recipient,
			"path" ->  Routes.admin.assignment.extension.review(assignment, extension.universityId)
		)))
		baseMessage
	}

}