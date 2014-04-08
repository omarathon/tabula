package uk.ac.warwick.tabula.data.model.notifications

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleItemNotification, Assignment, Notification}
import uk.ac.warwick.tabula.coursework.web.Routes
import org.joda.time.{Days, DateTime}
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

trait SubmissionReminder {
	self : Notification[_, Unit] with PreSaveBehaviour =>

	def deadline: DateTime
	def assignment: Assignment
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	def daysLeft = {
		val now = DateTime.now.withTimeAtStartOfDay()
		val closeDate = deadline.withTimeAtStartOfDay()
		Days.daysBetween(now, closeDate).getDays
	}

	override def preSave(newRecord: Boolean) {
		priority = if (daysLeft == 1) {
			Warning
		} else if (daysLeft < 1) {
			Critical
		} else {
			Info
		}
	}

	def actionRequired = true

	def url = Routes.assignment(assignment)

	def urlTitle = "upload your submission"

	def titleEnding = if(daysLeft < 0) { "isLate" } else { "due" }

	def title = s"$moduleCode assignment $titleEnding"

	def timeStatement = if (daysLeft > 1){
		s"is due in $daysLeft days"
	} else if (daysLeft == 1) {
		"is due tomorrow"
	} else if (daysLeft == 0) {
		"is due today"
	} else if (daysLeft == -1) {
		"is 1 day late"
	} else {
		s"is ${0 - daysLeft} days late"
	}

	def be = if (daysLeft >= 0) "is" else "was"
	def assignmentDate = be + " " + dateTimeFormatter.print(assignment.closeDate)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/submission_reminder.ftl", Map(
		"assignment" -> assignment,
		"module" -> module,
		"timeStatement" -> timeStatement,
		"assignmentDate" -> assignmentDate
	))

	def verb = "Remind"

}

@Entity
@DiscriminatorValue("SubmissionDueGeneral")
class SubmissionDueGeneralNotification extends Notification[Assignment, Unit] with SingleItemNotification[Assignment]
	with SubmissionReminder with PreSaveBehaviour {

	def deadline = assignment.closeDate
	def assignment = item.entity

	def recipients = {
		val submissions = assignment.submissions.asScala
		val extensions = assignment.extensions.asScala
		val allStudents = assignment.membershipInfo.items.map(_.user)
		// fist filter out students that have submitted already
		val withoutSubmission = allStudents.filterNot(user => submissions.exists(_.universityId == user.getWarwickId))
		// finally filter students that have an extension
		withoutSubmission.filterNot(user => extensions.exists(_.universityId == user.getWarwickId))
	}
}

@Entity
@DiscriminatorValue("SubmissionDueExtension")
class SubmissionDueWithExtensionNotification extends Notification[Extension, Unit] with SingleItemNotification[Extension]
	with SubmissionReminder with PreSaveBehaviour with AutowiringUserLookupComponent {

	def extension = item.entity

	def deadline = extension.expiryDate
	def assignment = extension.assignment

	def recipients = Seq(userLookup.getUserByWarwickUniId(extension.universityId))
}
