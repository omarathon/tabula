package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.coursework.web.Routes
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.FreemarkerModel
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning


abstract class ExtensionRequestRespondedNotification(val verbed: String) extends ExtensionNotification {

	def verb = "respond"

	def title = "%sExtension request by %s was %s".format(titlePrefix, student.getFullName, verbed)
	def url = Routes.admin.assignment.extension.review(assignment, extension.universityId)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/responded_extension_request.ftl", Map(
		"studentName" -> student.getFullName,
		"agentName" -> agent.getFullName,
		"verbed" -> verbed,
		"newExpiryDate" -> dateTimeFormatter.print(extension.expiryDate),
		"assignment" -> assignment,
		"path" ->  url
	))

	def recipients = assignment.module.department.extensionManagers.users.filterNot(_ == agent)
}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedApprove")
class ExtensionRequestRespondedApproveNotification extends ExtensionRequestRespondedNotification("approved") {}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedReject")
class ExtensionRequestRespondedRejectNotification extends ExtensionRequestRespondedNotification("rejected") {
	priority = Warning
}