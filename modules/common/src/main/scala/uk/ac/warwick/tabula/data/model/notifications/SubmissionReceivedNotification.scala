package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.userlookup.User
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{SecurityService, UserSettingsService}
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.permissions.{PermissionsTarget, Permissions}
import scala.reflect.ClassTag
import uk.ac.warwick.tabula.data.model.permissions.{GrantedPermission, RoleOverride}
import uk.ac.warwick.tabula.CurrentUser

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
	var userSettings = Wire[UserSettingsService]

	@transient
	var permissionsService = Wire[PermissionsService]

	@transient
	var securityService = Wire[SecurityService]

	def templateLocation = "/WEB-INF/freemarker/emails/submissionnotify.ftl"
	def submissionTitle =
		if (submission == null) "Submission"
		else if (submission.isAuthorisedLate) "Authorised Late Submission"
		else if(submission.isLate) "Late Submission"
		else "Submission"

	def title = moduleCode + ": " + submissionTitle

	def canEmailUser(user: User) : Boolean = {
		// Alert on noteworthy submissions by default
		val setting = userSettings.getByUserId(user.getUserId).map { _.alertsSubmission }.getOrElse(UserSettings.AlertsNoteworthySubmissions)

		setting match {
			case UserSettings.AlertsAllSubmissions => true
			case UserSettings.AlertsNoteworthySubmissions => submission.isNoteworthy
			case _ => false
		}
	}

	def url = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view all submissions for this assignment"

	def recipients = {
		// TAB-2333 Get any user that has Submission.Delete over the submission, or any of its permission parents
		// Look at Assignment, Module and Department (can't grant explicitly over one Submission atm)
		val requiredPermission = Permissions.Submission.Delete
		def usersWithPermission[A <: PermissionsTarget: ClassTag](scope: A) = {
			val roleGrantedUsers =
				permissionsService.getAllGrantedRolesFor[A](scope)
					.filter { _.mayGrant(requiredPermission) }
					.flatMap { _.users.users }
					.toSet

			val explicitlyGrantedUsers =
				permissionsService.getGrantedPermission(scope, requiredPermission, RoleOverride.Allow)
					.toSet
					.flatMap { permission: GrantedPermission[A] => permission.users.users }

			roleGrantedUsers ++ explicitlyGrantedUsers
		}

		val allAdmins = usersWithPermission(assignment) ++ usersWithPermission(module) ++ usersWithPermission(module.adminDepartment)
		allAdmins.toSeq
			.filter { user => securityService.can(new CurrentUser(user, user), requiredPermission, submission) }
			.filter(canEmailUser)
	}

	def actionRequired = false
}
