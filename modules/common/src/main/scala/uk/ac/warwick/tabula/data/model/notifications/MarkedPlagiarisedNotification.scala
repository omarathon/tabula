package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleItemNotification, Assignment, Submission, NotificationWithTarget}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

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
		val departmentAdmins = module.department.owners

		moduleManagers.users ++ departmentAdmins.users
	}

	def actionRequired = false

	def url = Routes.admin.assignment.submissionsandfeedback(assignment)

	def urlTitle = "view the submissions for this assignment"

	def title = s"$moduleCode: Plagiarism suspected"

	def verb = "Mark plagiarised"

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/suspectPlagiarism.ftl", Map(
		"submission" -> submission,
		"assignment" -> assignment,
		"module"	-> module,
		"student" -> student
	))
}
