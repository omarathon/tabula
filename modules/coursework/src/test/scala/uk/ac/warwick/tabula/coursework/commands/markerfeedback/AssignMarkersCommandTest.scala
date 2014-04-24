package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConverters._

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.services.{AssignmentServiceComponent, AssignmentService}
import uk.ac.warwick.tabula.data.{UserGroupDao, UserGroupDaoComponent}

// scalastyle:off magic.number
class AssignMarkersCommandTest extends TestBase with Mockito {

	@Test
	def assignMarkers() { new MarkingWorkflowWorld {
		val command = new AssignMarkersCommand(assignment.module, assignment) with AssignmentServiceComponent with UserGroupDaoComponent {
			val assignmentService = smartMock[AssignmentService]
			val userGroupDao = smartMock[UserGroupDao]
		}

		command.firstMarkerMapping = Map(
			"cuslaj" -> List("cusebr", "cuscav").asJava,
			"cuscav" -> List("cusxad", "cuscao", "curef").asJava,
			"cuslat" -> List("cusxad", "cuscao", "curef", "cusebr").asJava,
			"cuday" -> List("cuscav").asJava
		).asJava

		command.applyInternal()

		assignment.firstMarkerMap.get("cuslaj").get.size should be(2)
		assignment.firstMarkerMap.get("cuscav").size should be(3)
		assignment.firstMarkerMap.get("cuslat").size should be(4)
		assignment.firstMarkerMap.get("cuday").size should be(1)

	} }

}
