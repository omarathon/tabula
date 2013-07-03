package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.views.FreemarkerTextRenderer
import uk.ac.warwick.tabula.commands.{Notifies, Command, ReadOnly, Description}
import uk.ac.warwick.tabula.services.UserLookupService
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.system.permissions.Public
import language.implicitConversions
import scala.Some
import uk.ac.warwick.tabula.coursework.commands.assignments.notifications.SubmissionRecievedNotification

class SendSubmissionNotifyCommand (val submission: Submission, val users: UserGroup)
	extends Command[Boolean] with Notifies[Submission] with ReadOnly with Public {

	mandatory(submission)
	
	var userLookup = Wire.auto[UserLookupService]
	var userSettings = Wire.auto[UserSettingsService]
	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	var assignment: Assignment = submission.assignment
	var module: Module = assignment.module
	var student:User = userLookup.getUserByUserId(submission.userId)
	var admins: Seq[User] = _

	def applyInternal() = {
		val userIds = users.includeUsers
		val allAdmins = userIds.map(userLookup.getUserByUserId(_))
		admins = allAdmins.filter(canEmailUser(_))
		true
	}
	
	def canEmailUser(user: User) : Boolean = {
		userSettings.getByUserId(user.getUserId) match {
			case Some(settings) => settings.alertsSubmission match {
				case UserSettings.AlertsAllSubmissions => true
				case UserSettings.AlertsLateSubmissions => submission.isLate || submission.isAuthorisedLate
				case _ => false
			}
			case None => false
		}
	}
	
	override def describe(d: Description) {
		d.assignment(assignment)
		.submission(submission)
	}

	def emit: Seq[Notification[Submission]] = {
		if(admins.size == 0){
			Nil
		} else {
			Seq(new SubmissionRecievedNotification(submission, student, admins) with FreemarkerTextRenderer)
		}
	}

}