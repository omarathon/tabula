package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.LocalDate
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Info, Warning}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

trait FeedbackDueNotification {

	self : Notification[_, Unit] with NotificationPreSaveBehaviour =>

	protected def deadline: LocalDate
	protected def assignment: Assignment

	@transient private lazy val workingDaysHelper = new WorkingDaysHelperImpl

	protected def daysLeft = {
		workingDaysHelper.getNumWorkingDays(created.toLocalDate, deadline)
	}

	override final def onPreSave(newRecord: Boolean) {
		priority = if (daysLeft == 1) {
			Warning
		} else if (daysLeft < 1) {
			Critical
		} else {
			Info
		}
	}

	override final def actionRequired = true

	override final def verb = "publish"

	override final def urlTitle = "publish this feedback"

	override def url = Routes.admin.assignment.submissionsandfeedback(assignment)

}

@Entity
@DiscriminatorValue("FeedbackDueGeneral")
class FeedbackDueGeneralNotification
	extends Notification[Assignment, Unit] with SingleItemNotification[Assignment] with FeedbackDueNotification {

	override final def assignment = item.entity

	override final def title = s"${assignment.module.code.toUpperCase} feedback due"

	def moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	override final def recipients = {
		val module = moduleAndDepartmentService.getModuleByCode(assignment.module.code).getOrElse(throw new IllegalStateException("No such module"))
		val managers = module.managers
		managers.users
	}

	override final def deadline = assignment.feedbackDeadline.getOrElse(throw new IllegalStateException("No feedback deadline for open-ended assignments"))

	override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_general.ftl", Map(
		"assignment" -> assignment,
		"daysLeft" -> daysLeft,
		"dateOnlyFormatter" -> dateOnlyFormatter,
		"deadline" -> deadline
	))
}
