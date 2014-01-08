package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer

class ExtensionRequestApprovedNotification (extension:Extension, student: User, actor: User)
	extends ExtensionStudentNotification(Some(extension), extension.assignment, student, actor){

	this: TextRenderer =>

	val verb = "approve"
	def title: String = titlePrefix + "Extension request approved"

	val template = "/WEB-INF/freemarker/emails/extension_request_approved.ftl"
	val contentModel:Map[String, Any] = contentBaseModel ++ Map(
		"extension" -> extension,
		"newExpiryDate" -> dateTimeFormatter.print(extension.expiryDate)
	)
}
