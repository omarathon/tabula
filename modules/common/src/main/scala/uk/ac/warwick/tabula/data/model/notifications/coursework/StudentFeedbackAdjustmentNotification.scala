package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Info
import uk.ac.warwick.tabula.data.model._

import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object StudentFeedbackAdjustmentNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/student_feedback_adjustment_notification.ftl"
}

@Entity
@DiscriminatorValue("StudentFeedbackAdjustment")
class StudentFeedbackAdjustmentNotification
	extends NotificationWithTarget[AssignmentFeedback, Assignment]
	with SingleItemNotification[AssignmentFeedback]
	with SingleRecipientNotification
	with AutowiringUserLookupComponent {

	def verb = "adjusted"
	def assignment = target.entity
	def feedback: Feedback = item.entity

	def recipient = {
		val uniId = Option(feedback.universityId).getOrElse({
			throw new IllegalStateException(s"No student found for ${feedback.universityId}")
		})
		userLookup.getUserByWarwickUniId(uniId)
	}

	def whatAdjusted = {
		val mark = feedback.adjustedMark.map(m => "mark")
		val grade = feedback.adjustedGrade.map(g => "grade")
		(mark ++ grade).mkString(" and ")
	}

	def title = s"${assignment.module.code.toUpperCase} - for ${assignment.name} : Adjustments have been made to your $whatAdjusted"

	def content = FreemarkerModel(StudentFeedbackAdjustmentNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"feedback" -> feedback,
			"whatAdjusted" -> whatAdjusted
		))

	def url: String = Routes.admin.assignment.markerFeedback(assignment, recipient)
	def urlTitle = s"Marking for this assignment"

	priority = Info

}
