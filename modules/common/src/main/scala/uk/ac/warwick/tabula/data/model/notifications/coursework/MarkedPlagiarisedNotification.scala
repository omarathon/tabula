package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{Assignment, FreemarkerModel, NotificationWithTarget, SingleItemNotification, Submission}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

@Entity
@DiscriminatorValue(value="MarkedPlagarised")
class MarkedPlagiarisedNotification extends NotificationWithTarget[Submission, Assignment]
	with SingleItemNotification[Submission] with AutowiringUserLookupComponent {

	def submission = item.entity
	def assignment = target.entity
	def module = assignment.module
	def moduleCode = module.code.toUpperCase
	def student = userLookup.getUserByWarwickUniId(submission.universityId)

	priority = Warning

	def recipients = {
		val moduleManagers = module.managers
		val departmentAdmins = module.adminDepartment.owners

		moduleManagers.users ++ departmentAdmins.users
	}

	def actionRequired = false

	def url = Routes.admin.assignment.submissionsandfeedback(assignment)

	def urlTitle = "view the submissions for this assignment"

	def title = "%s: A submission by %s for \"%s\" is suspected of plagiarism".format(moduleCode, submission.universityId, assignment.name)

	def verb = "Mark plagiarised"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/suspectPlagiarism.ftl", Map(
		"submission" -> submission,
		"assignment" -> assignment,
		"module"	-> module,
		"student" -> student
	))
}
