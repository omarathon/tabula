package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

@Entity
@DiscriminatorValue(value="FeedbackChange")
class FeedbackChangeNotification extends NotificationWithTarget[Feedback, Assignment]
	with SingleItemNotification[Feedback]
	with SingleRecipientNotification
	with UniversityIdRecipientNotification
	with AutowiringUserLookupComponent
	with AllCompletedActionRequiredNotification {

	def feedback = item.entity
	def assignment = target.entity
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	priority = Warning

	override def onPreSave(newRecord: Boolean) {
		recipientUniversityId = feedback.universityId
	}

	def verb = "modify"

	def title = "%s: Your assignment feedback for \"%s\" has been updated".format(moduleCode, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/feedbackchanged.ftl", Map(
		"assignment" -> assignment,
		"module" -> module
	))

	def url = Routes.assignment.receipt(assignment)
	def urlTitle = "view your new feedback"

}
