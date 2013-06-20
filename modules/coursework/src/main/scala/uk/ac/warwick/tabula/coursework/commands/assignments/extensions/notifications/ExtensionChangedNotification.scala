package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User

class ExtensionChangedNotification(extension:Extension, student: User, actor: User)
	extends ExtensionStudentNotification(Some(extension), extension.assignment, student, actor){

	this: TextRenderer =>

	val verb = "updated"
	def title: String = titleHeading + "Extension details have been changed"

	val contentModel:Map[String, Any] = contentBaseModel ++ Map(
		"extension" -> extension,
		"newExpiryDate" -> dateFormatter.print(extension.expiryDate)
	)

	def content: String = {
		renderTemplate("/WEB-INF/freemarker/emails/modified_manual_extension.ftl", contentModel)
	}

	def recipients = Seq(student)
}
