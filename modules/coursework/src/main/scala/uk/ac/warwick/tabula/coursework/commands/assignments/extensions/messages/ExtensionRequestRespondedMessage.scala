package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.messages

import uk.ac.warwick.tabula.data.model.forms.Extension
import org.springframework.mail.javamail.MimeMessageHelper
import uk.ac.warwick.tabula.coursework.web.Routes

/**
 * Sent to other admins when someone responds to an extension request to let them know whats happening
 */
class ExtensionRequestRespondedMessage (extension: Extension, userId: String, val approverId: String)
	extends ExtensionMessage(extension: Extension, userId: String) {

	// applied to a base message to set a context specific subject and body
	override def setMessageContent(baseMessage: MimeMessageHelper) {

		val verbed = if(extension.approved) "approved" else "rejected"
		val studentName = userLookup.getUserByWarwickUniId(extension.universityId).getFullName

		baseMessage.setSubject(encodeSubject("%sExtension request by %s was %s" format (getSubjectPrefix, studentName, verbed)))
		baseMessage.setText(renderToString("/WEB-INF/freemarker/emails/responded_extension_request.ftl", Map(
			"extension" -> extension,
			"studentName" -> studentName,
			"approverName" -> userLookup.getUserByUserId(approverId).getFullName,
			"verbed" -> verbed,
			"newExpiryDate" -> dateFormatter.print(extension.expiryDate),
			"assignment" -> assignment,
			"module" -> module,
			"user" -> recipient,
			"path" ->  Routes.admin.assignment.extension.review(assignment, extension.universityId)
		)))
	}

}
