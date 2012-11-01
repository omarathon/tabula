package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import scala.reflect.BeanProperty

/** Simply marks an assignment as archived. */
class ArchiveAssignmentCommand(val assignment: Assignment) extends Command[Unit] {

	@BeanProperty var unarchive: Boolean = false

	@Transactional
	def work {
		assignment.archived = !unarchive
	}

	def describe(description: Description) = description
		.assignment(assignment)
		.property("unarchive" -> unarchive)

}