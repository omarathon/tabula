package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.views.TextRenderer

class ExtensionRevokedNotification(assignment:Assignment, student: User, actor: User)
	extends ExtensionStudentNotification(None, assignment, student, actor){

	this: TextRenderer =>

	val verb = "revoke"
	def title: String = titleHeading + "Extension revoked"

	val contentModel:Map[String, Any] = contentBaseModel ++ Map(
		"originalAssignmentDate" -> dateFormatter.print(assignment.closeDate)
	)

	def content: String = {
		renderTemplate("/WEB-INF/freemarker/emails/revoke_manual_extension.ftl", contentModel)
	}

	def recipients = Seq(student)

}
