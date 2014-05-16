package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.userlookup.User
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

@Entity
@DiscriminatorValue("SubmissionReceived")
class SubmissionReceivedNotification extends SubmissionNotification {

	override def onPreSave(isNew: Boolean) {
		// if this submission was noteworthy then the priority is higher
		if (submission.isNoteworthy) {
			priority = Warning
		}
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
				case UserSettings.AlertsNoteworthySubmissions => submission.isNoteworthy
				case _ => false
			}
			case None => false
		}
	}

	def url = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view all submissions for this assignment"

	def recipients = {
		val moduleManagers = module.managers
		val departmentAdmins = module.department.owners

		val allAdmins = moduleManagers.users ++ departmentAdmins.users
		allAdmins.filter(canEmailUser)
	}

	def actionRequired = false
}
