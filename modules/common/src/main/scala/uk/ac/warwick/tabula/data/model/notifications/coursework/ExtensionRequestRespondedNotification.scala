package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.FreemarkerModel


abstract class ExtensionRequestRespondedNotification(val verbed: String) extends ExtensionNotification {

	def verb = "respond"

	def title = titlePrefix + "Extension request by %s for \"%s\" was %s".format(student.getFullName, assignment.name, verbed)

	def url = Routes.admin.assignment.extension.expandrow(assignment, student.getWarwickId)
	def urlTitle = "review this extension request"

	def contentMap = Map(
		"studentName" -> student.getFullName,
		"agentName" -> agent.getFullName,
		"verbed" -> verbed,
		"assignment" -> assignment,
		"path" ->  url
	)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/responded_extension_request.ftl", contentMap)

	def recipients = assignment.module.adminDepartment.extensionManagers.users.filterNot(_ == agent)

}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedApprove")
class ExtensionRequestRespondedApproveNotification extends ExtensionRequestRespondedNotification("approved") {
	override def contentMap = {
		val map = super.contentMap
		val expiryDate = extension.expiryDate.getOrElse(
			throw new IllegalArgumentException("Can't send an extension approval notification without an expiry date")
		)
		map + ("newExpiryDate" -> dateTimeFormatter.print(expiryDate))
	}
}

@Entity
@DiscriminatorValue("ExtensionRequestRespondedReject")
class ExtensionRequestRespondedRejectNotification extends ExtensionRequestRespondedNotification("rejected") {}