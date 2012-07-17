package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional

/** Simply marks an assignment as archived. */
class ArchiveAssignmentCommand(val assignment: Assignment) extends Command[Unit] {

	@Transactional
	def apply {
		assignment.archived = true
	}
	
	def describe(description:Description) = description.assignment(assignment)
	
}