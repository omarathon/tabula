package uk.ac.warwick.tabula.data.model.notifications.exams

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.exams.web
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, FeedbackService}
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue(value="ExamMarked")
class ExamMarkedNotification
	extends NotificationWithTarget[ExamFeedback, Exam] {

	@transient
	final lazy val exam = target.entity

	@transient
	final lazy val moduleCode = exam.module.code.toUpperCase

	@transient
	var assessmentMembershipService = Wire[AssessmentMembershipService]

	@transient
	var feedbackService = Wire[FeedbackService]

	@transient
	final lazy val students = assessmentMembershipService.determineMembershipUsersWithOrder(exam).map(_._1)

	@transient
	final lazy val allFeedbacks = feedbackService.getExamFeedbackMap(exam, students).values.toSeq.filter(_.latestMark.isDefined)

	// TODO - Neither of these two things should be necessary as the collection of marked feedbacks is what should be in 'items'
	// but Hibernate is refusing to hydrate the entity of each ExamFeedbackEntityReference (they're always null); I don't know why
	@transient
	final lazy val markerStudents = assessmentMembershipService.determineMembershipUsersWithOrderForMarker(exam, agent).map(_._1)

	@transient
	final lazy val markerFeedbacks = feedbackService.getExamFeedbackMap(exam, markerStudents).values.toSeq.filter(_.latestMark.isDefined)

	override final def onPreSave(newRecord: Boolean) {
		priority = if (allFeedbacks.size >= students.size) {
			NotificationPriority.Warning
		} else {
			NotificationPriority.Info
		}
	}

	def verb = "view"

	def title = "%s: Exam marks added for \"%s\"".format(moduleCode, exam.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/notifications/exams/exam_marker_marked.ftl", Map(
			"examName" -> exam.name,
			"moduleCode" -> moduleCode,
			"marker" -> agent,
			"feedbacks" -> markerFeedbacks,
			"finalMarks" -> (allFeedbacks.size >= students.size)
		))


	def url = web.Routes.Exams.admin.exam(exam)
	def urlTitle = "view the exam"

	def recipients = exam.module.adminDepartment.owners.users
		.filter(admin => admin.isFoundUser && admin.getEmail.hasText)

}
