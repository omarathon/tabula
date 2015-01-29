package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

@Entity
@DiscriminatorValue(value="FeedbackPublished")
class FeedbackPublishedNotification
	extends NotificationWithTarget[Feedback, Assignment]
	with SingleRecipientNotification
	with SingleItemNotification[Feedback]
	with UniversityIdRecipientNotification
	with AutowiringUserLookupComponent
	with AllCompletedActionRequiredNotification {

	def feedback = item.entity
	def assignment = feedback.assignment
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	priority = Warning

	def verb = "publish"

	def title = "%s: Your assignment feedback for \"%s\" is now available".format(moduleCode, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/feedbackready.ftl", Map(
		"assignmentName" -> assignment.name,
		"moduleCode" -> assignment.module.code.toUpperCase,
		"moduleName" -> assignment.module.name,
		"path" -> url
	))

	def url = Routes.assignment.receipt(assignment)
	def urlTitle = "view your feedback"

}
