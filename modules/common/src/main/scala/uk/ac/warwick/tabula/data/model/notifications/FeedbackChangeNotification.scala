package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

@Entity
@DiscriminatorValue(value="FeedbackChange")
class FeedbackChangeNotification extends NotificationWithTarget[Feedback, Assignment]
	with SingleItemNotification[Feedback]
	with SingleRecipientNotification
	with UniversityIdRecipientNotification
	with AutowiringUserLookupComponent {

	def feedback = item.entity
	def assignment = target.entity
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	priority = Warning

	override def onPreSave(newRecord: Boolean) {
		recipientUniversityId = feedback.universityId
	}

	def verb = "modify"

	def title = s"$moduleCode: Feedback updated"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/feedbackchanged.ftl", Map(
		"assignment" -> assignment,
		"module" -> module
	))

	def url = Routes.assignment.receipt(assignment)
	def urlTitle = "view your new feedback"
	def actionRequired = true
}
