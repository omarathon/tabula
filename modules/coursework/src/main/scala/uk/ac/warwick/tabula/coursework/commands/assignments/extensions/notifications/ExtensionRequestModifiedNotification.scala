package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

class ExtensionRequestModifiedNotification(extension:Extension, student:User)
	extends ExtensionRequestNotification(extension, student) {

	this: TextRenderer =>

	val verb = "modify"
	val template = "/WEB-INF/freemarker/emails/modified_extension_request.ftl"
	def title = titlePrefix + "Extension request modified"
}