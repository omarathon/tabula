package uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications

import uk.ac.warwick.tabula.data.model.{Assignment, Notification}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.userlookup.User

abstract class ExtensionNotification(val _object: Option[Extension], val assignment: Assignment, val agent :User)
	extends Notification[Option[Extension]] {

	this: TextRenderer =>

	val target = Some(assignment)
	def titlePrefix = assignment.module.code.toUpperCase + ": "
}
