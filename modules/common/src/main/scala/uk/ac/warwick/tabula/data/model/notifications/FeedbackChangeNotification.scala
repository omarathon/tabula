package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import javax.persistence.{Entity, DiscriminatorValue}

@Entity
@DiscriminatorValue(value="FeedbackChange")
class FeedbackChangeNotification extends NotificationWithTarget[Feedback, Assignment]
	with SingleItemNotification[Feedback]
	with SingleRecipientNotification
	with UniversityIdRecipientNotification
	with AutowiringUserLookupComponent
	with PreSaveBehaviour {

	def feedback = item.entity
	def assignment = target.entity
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	override def preSave(newRecord: Boolean) {
		recipientUniversityId = feedback.universityId
	}

	def verb = "modify"

	def title = s"$moduleCode: Feedback updated"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/feedbackchanged.ftl", Map(
		"assignment" -> assignment,
		"module" -> module,
		"path" -> url
	))

	def url = Routes.assignment.receipt(assignment)
}
