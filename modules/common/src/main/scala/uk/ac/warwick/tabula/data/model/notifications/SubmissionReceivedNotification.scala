package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.userlookup.User
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

@Entity
@DiscriminatorValue("SubmissionReceived")
class SubmissionReceivedNotification extends SubmissionNotification with PreSaveBehaviour {

	override def preSave(isNew: Boolean) {
		// if this submission was late then the priority is higher
		if (submission.isLate) priority = Warning
	}

	@transient
	var userSettings = Wire.auto[UserSettingsService]

	def templateLocation = "/WEB-INF/freemarker/emails/submissionnotify.ftl"
	def submissionTitle =
		if(submission.isAuthorisedLate) "Authorised Late Submission"
		else if(submission.isLate) "Late Submission"
		else "Submission"

	def title = moduleCode + ": " + submissionTitle

	def canEmailUser(user: User) : Boolean = {
		userSettings.getByUserId(user.getUserId) match {
			case Some(s) => s.alertsSubmission match {
				case UserSettings.AlertsAllSubmissions => true
				case UserSettings.AlertsLateSubmissions => submission.isLate || submission.isAuthorisedLate
				case _ => false
			}
			case None => false
		}
	}

	def url = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view all submissions for this assignment"

	def recipients = {
		val moduleManagers = submission.assignment.module.managers
		val allAdmins = moduleManagers.users
		allAdmins.filter(canEmailUser)
	}
}
