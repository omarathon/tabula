package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, UniversityIdRecipientNotification, SingleRecipientNotification, SingleItemNotification, Assignment, Notification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

object ExtensionRevokedNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/revoke_manual_extension.ftl"
}

@Entity
@DiscriminatorValue("ExtensionRevoked")
class ExtensionRevokedNotification extends Notification[Assignment, Unit]
	with SingleItemNotification[Assignment]
	with SingleRecipientNotification
	with UniversityIdRecipientNotification
	with AutowiringUserLookupComponent {

	priority = Warning
	def actionRequired = false

	def assignment = item.entity

	def verb = "revoke"

	def title =	"Extended deadline revoked"
	def url = Routes.assignment.apply(assignment)
	def urlTitle = "view the assignment"

	def content = FreemarkerModel(ExtensionRevokedNotification.templateLocation, Map (
		"assignment" -> assignment,
		"module" -> assignment.module,
		"user" -> recipient,
		"originalAssignmentDate" -> dateTimeFormatter.print(assignment.closeDate)
	))
}