package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.web.Routes

class ExtensionRequestRespondedNotification(extension: Extension, student: User, agent: User)
	extends ExtensionNotification(Some(extension), extension.assignment, agent) {

	this: TextRenderer =>

	val verb = "respond"

	val verbed = if(extension.approved) "approved" else "rejected"

	def title = "%sExtension request by %s was %s".format(titlePrefix, student.getFullName, verbed)
	def url = Routes.admin.assignment.extension.review(assignment, extension.universityId)

	def content = renderTemplate("/WEB-INF/freemarker/emails/responded_extension_request.ftl", Map(
		"studentName" -> student.getFullName,
		"agentName" -> agent.getFullName,
		"verbed" -> verbed,
		"newExpiryDate" -> dateFormatter.print(extension.expiryDate),
		"assignment" -> assignment,
		"path" ->  url
	))

	def recipients = {
		extension.assignment.module.department.extensionManagers.users.filterNot(_ == agent)
	}
}
