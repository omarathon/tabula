package uk.ac.warwick.tabula.data.model.notifications

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.userlookup.User
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserSettingsService

@Entity
@DiscriminatorValue("SubmissionReceived")
class SubmissionReceivedNotification extends SubmissionNotification {

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

	def recipients = {
		val moduleManagers = submission.assignment.module.managers
		val userIds = moduleManagers.includeUsers
		val allAdmins = userIds.asScala.map(id => userLookup.getUserByUserId(id))
		allAdmins.filter(canEmailUser)
	}
}
