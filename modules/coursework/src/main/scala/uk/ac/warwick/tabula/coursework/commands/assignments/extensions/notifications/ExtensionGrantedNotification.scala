package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.userlookup.User

class ExtensionGrantedNotification(extension:Extension, student: User, actor: User)
	extends ExtensionStudentNotification(Some(extension), extension.assignment, student, actor){

	this: TextRenderer =>

	val verb = "grant"

	def title: String = titleHeading + "Extension granted"

	val contentModel:Map[String, Any] = contentBaseModel ++ Map(
		"extension" -> extension,
		"newExpiryDate" -> dateFormatter.print(extension.expiryDate)
	)

	def content: String = {
		renderTemplate("/WEB-INF/freemarker/emails/new_manual_extension.ftl", contentModel)
	}

	def recipients = Seq(student)
}
