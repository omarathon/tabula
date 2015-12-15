package uk.ac.warwick.tabula.data.model.notifications.exams

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.web
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

@Entity
@DiscriminatorValue(value="ExamReleased")
class ExamReleasedForMarkingNotification extends Notification[Exam, Unit]
	with SingleItemNotification[Exam]
	with SingleRecipientNotification
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	@transient
	final lazy val exam = item.entity

	@transient
	final lazy val moduleCode = exam.module.code.toUpperCase

	def verb = "released"

	def title = s"$moduleCode - ${exam.name} has been released for marking"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/exam_released_to_marker_notification.ftl", Map(
		"exam" -> exam
	))

	def url = web.Routes.Exams.admin.markerFeedback(exam, recipient)
	def urlTitle = "enter marks"

}