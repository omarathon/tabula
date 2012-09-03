package uk.ac.warwick.courses.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.Description

class AddExtensionCommand(val assignment:Assignment, val submitter: CurrentUser) extends ModifyExtensionCommand {

	def describe(d: Description) {
		d.assignment(assignment)
		d.module(assignment.module)
		d.studentIds(extensionItems map (_.universityId))
	}
}
