package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages
import org.springframework.mail.javamail.MimeMessageHelper

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.forms.Extension

class ModifiedExtensionRequestMessage(extension: Extension, userId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	override def setMessageContent(baseMessage: MimeMessageHelper) {
		baseMessage.setSubject(encodeSubject(getSubjectPrefix + "Extension request modified"))
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/modified_extension_request.ftl", Map(
			"requestedExpiryDate" -> dateFormatter.print(extension.requestedExpiryDate),
			"reasonForRequest" -> extension.reason,
			"attachments" -> extension.attachments,
			"assignment" -> assignment,
			"student" -> userLookup.getUserByUserId(extension.userId),
			"module" -> module,
			"user" -> recipient,
			"path" -> Routes.admin.assignment.extension.review(assignment, extension.universityId)
		)))
	}

}