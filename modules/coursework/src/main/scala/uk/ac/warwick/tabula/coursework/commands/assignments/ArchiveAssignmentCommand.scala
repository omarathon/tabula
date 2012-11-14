package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.coursework.commands._
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.services.AssignmentService
import scala.reflect.BeanProperty

/** Simply marks an assignment as archived. */
class ArchiveAssignmentCommand(val assignment: Assignment) extends Command[Unit] {

	@BeanProperty var unarchive: Boolean = false

	def work {
		transactional() {
			assignment.archived = !unarchive
		}
	}

	def describe(description: Description) = description
		.assignment(assignment)
		.property("unarchive" -> unarchive)

}