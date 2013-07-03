package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer

class ExtensionRequestCreatedNotification(extension:Extension, student:User)
	extends ExtensionRequestNotification(extension, student) {

	this: TextRenderer =>

	val verb = "create"
	val template = "/WEB-INF/freemarker/emails/new_extension_request.ftl"
	def title = titlePrefix + "New extension request made"
}