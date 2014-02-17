package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import uk.ac.warwick.tabula.coursework.web.Routes
import javax.persistence.{Entity, DiscriminatorValue}

abstract class ExtensionStudentNotification extends ExtensionNotification with SingleRecipientNotification {

	def recipient = student
	def url = Routes.assignment.apply(assignment)
	def template: String

	def content = FreemarkerModel(template, Map (
			"assignment" -> assignment,
			"module" -> assignment.module,
			"user" -> recipient,
			"path" -> url,
			"extension" -> extension,
			"newExpiryDate" -> dateTimeFormatter.print(extension.expiryDate),
			"originalAssignmentDate" -> dateTimeFormatter.print(assignment.closeDate)
	))
}

@Entity
@DiscriminatorValue("ExtensionChanged")
class ExtensionChangedNotification extends ExtensionStudentNotification {
	def verb = "updated"
	def title = titlePrefix + "Extension details have been changed"
	def template = "/WEB-INF/freemarker/emails/modified_manual_extension.ftl"
}

@Entity
@DiscriminatorValue("ExtensionGranted")
class ExtensionGrantedNotification extends ExtensionStudentNotification {
	def verb = "grant"
	def title = titlePrefix + "Extension granted"
	def template = "/WEB-INF/freemarker/emails/new_manual_extension.ftl"
}

@Entity
@DiscriminatorValue("ExtensionRequestApproved")
class ExtensionRequestApprovedNotification extends ExtensionStudentNotification {
	def verb = "approve"
	def title = titlePrefix + "Extension request approved"
	def template = "/WEB-INF/freemarker/emails/extension_request_approved.ftl"
}

@Entity
@DiscriminatorValue("ExtensionRequestRejected")
class ExtensionRequestRejectedNotification extends ExtensionStudentNotification {
	def verb = "reject"
	def title = titlePrefix + "Extension request rejected"
	def template = "/WEB-INF/freemarker/emails/extension_request_rejected.ftl"
}