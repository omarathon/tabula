package uk.ac.warwick.tabula.coursework.jobs.notifications

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.services.turnitin.{ClassName, ClassId}

class TurnitinClassDeletedNotification(assignment: Assignment, user: User, className: ClassName, classId: ClassId)
	extends TurnitinReportNotification(Nil, assignment, user) {

	this: TextRenderer =>

	def title = "Turnitin check has not completed successfully for %s - %s" format (assignment.module.code.toUpperCase, assignment.name)

	def content = renderTemplate("/WEB-INF/freemarker/emails/turnitinclassdeleted.ftl", Map(
		"assignment" -> assignment,
		"assignmentTitle" -> ("%s - %s" format (assignment.module.code.toUpperCase, assignment.name)),
		"user" -> user,
		"path" -> url,
		"className" -> className,
		"classId" -> classId
	))
}
