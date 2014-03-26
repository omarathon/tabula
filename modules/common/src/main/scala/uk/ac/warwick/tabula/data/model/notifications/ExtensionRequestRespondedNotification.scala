package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.coursework.web.Routes
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.FreemarkerModel


abstract class ExtensionRequestRespondedNotification(val verbed: String) extends ExtensionNotification {

	def verb = "respond"

	def title = s"${titlePrefix}Extension request by ${student.getFullName} was $verbed"

	def url = Routes.admin.assignment.extension.detail(assignment)
	def urlTitle = "review this extension request"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/responded_extension_request.ftl", Map(
		"studentName" -> student.getFullName,
		"agentName" -> agent.getFullName,
		"verbed" -> verbed,
		"newExpiryDate" -> dateTimeFormatter.print(extension.expiryDate),
		"assignment" -> assignment,
		"path" ->  url
	))

	def recipients = assignment.module.department.extensionManagers.users.filterNot(_ == agent)
	def actionRequired = false
}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedApprove")
class ExtensionRequestRespondedApproveNotification extends ExtensionRequestRespondedNotification("approved") {}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedReject")
class ExtensionRequestRespondedRejectNotification extends ExtensionRequestRespondedNotification("rejected") {}