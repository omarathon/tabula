package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.web.views.TextRenderer

abstract class ExtensionStudentNotification(extension: Option[Extension], assignment:Assignment, student: User, agent:User)
	extends ExtensionNotification(extension, assignment, agent) {

	this: TextRenderer =>

	val contentBaseModel:Map[String, Any] = Map(
		"assignment" -> assignment,
		"module" -> assignment.module,
		"user" -> student,
		"path" -> url
	)


	def url = Routes.assignment.apply(assignment)
	def recipients = Seq(student)

	val template: String
	val contentModel: Map[String, Any]

	def content: String = {
		renderTemplate(template, contentModel)
	}

}