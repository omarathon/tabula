package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.Fixtures

class ArchiveAssignmentsTest  extends TestBase with Mockito {

	trait CommandTestSupport extends AssignmentServiceComponent {
		val assignmentService = mock[AssignmentService]
		def apply(): Seq[Assignment] = Seq()
	}

	trait Fixture {
		val department = Fixtures.department("bs")
		val module = Fixtures.module("bs101")

		val assignment = Fixtures.assignment("Essay 1")
		assignment.archived = true
	}

	@Test
	def commandApply() {
		new Fixture {
			val command = new ArchiveAssignmentsCommand(department, Seq(module)) with CommandTestSupport

			command.assignments = Seq(assignment)
			assignment.archived.booleanValue should be(true)
			command.applyInternal()
			verify(command.assignmentService, never).save(assignment)

			assignment.archived = false
			command.applyInternal()
			there was one(command.assignmentService).save(assignment)
			assignment.archived.booleanValue should be(true)

		}
	}

}

