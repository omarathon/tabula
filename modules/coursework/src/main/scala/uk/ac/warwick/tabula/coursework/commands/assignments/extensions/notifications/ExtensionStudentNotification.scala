package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.{Assignment, Notification}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.userlookup.User

abstract class ExtensionStudentNotification(val _object: Option[Extension], val assignment:Assignment, val student: User, val agent:User)
	extends Notification[Option[Extension]] {

	val target = Some(assignment)

	val contentBaseModel:Map[String, Any] = Map(
		"assignment" -> assignment,
		"module" -> assignment.module,
		"user" -> student,
		"path" -> url
	)

	def titleHeading = assignment.module.code.toUpperCase + ": "
	def url = Routes.assignment.apply(assignment)

}