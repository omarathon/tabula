package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.coursework.web.Routes

abstract class ExtensionRequestNotification(val extension:Extension, val student:User)
	extends ExtensionNotification(Some(extension), extension.assignment, student) {

	this: TextRenderer =>

	val template: String

	def url = Routes.admin.assignment.extension.review(assignment, extension.universityId)

	def content = renderTemplate(template, Map(
		"requestedExpiryDate" -> dateFormatter.print(extension.requestedExpiryDate),
		"reasonForRequest" -> extension.reason,
		"attachments" -> extension.attachments,
		"assignment" -> assignment,
		"student" -> student,
		"path" -> url
	))

	def recipients = extension.assignment.module.department.extensionManagers.users

}
