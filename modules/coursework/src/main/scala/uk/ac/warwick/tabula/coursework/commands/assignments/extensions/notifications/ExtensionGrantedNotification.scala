package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer

class ExtensionGrantedNotification(extension:Extension, student: User, actor: User)
	extends ExtensionStudentNotification(Some(extension), extension.assignment, student, actor){

	this: TextRenderer =>

	val verb = "grant"

	def title: String = titleHeading + "Extension granted"

	val template = "/WEB-INF/freemarker/emails/new_manual_extension.ftl"

	val contentModel:Map[String, Any] = contentBaseModel ++ Map(
		"extension" -> extension,
		"newExpiryDate" -> dateFormatter.print(extension.expiryDate)
	)
}
