package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import java.util.HashMap

import collection.JavaConverters._

import org.hibernate.Session
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, AssignmentService, UserLookupService}
import uk.ac.warwick.tabula.data.SessionComponent

// scalastyle:off magic.number
class AssignMarkersTest extends TestBase with Mockito {

	@Test
	def assignMarkers() { new MarkingWorkflowWorld {
		val command = new AbstractAssignMarkersCommand(assignment.module, assignment) with SessionComponent {
			val session = smartMock[Session]
		}

		val userLookup = new MockUserLookup
		userLookup.registerUsers("cuslaj", "cusxad", "cuscao", "curef", "cuscav", "cusebr", "cuslat", "cuday")

		command.userLookup = userLookup
		command.assignmentService = smartMock[AssignmentService]
		command.assignmentMembershipService = smartMock[AssignmentMembershipService]

		// mock expectations
		command.assignmentMembershipService.determineMembershipUsers(assignment) returns (Nil)
		// end expectations

		assignment.markerMap.asScala.foreach { case (_, group) => group.userLookup = userLookup }

		command.onBind()

		command.firstMarkers.size should be (2)
		command.secondMarkers.size should be (2)

		command.firstMarkers.asScala.find(_.userCode == "cuslaj").get.students.size should  be (3)
		command.firstMarkers.asScala.find(_.userCode == "cuscav").get.students.size should  be (2)
		command.secondMarkers.asScala.find(_.userCode == "cuslat").get.students.size should  be (3)
		command.secondMarkers.asScala.find(_.userCode == "cuday").get.students.size should  be (2)


		// students without a marker should be empty
		command.firstMarkerUnassignedStudents should be ('empty)
		command.secondMarkerUnassignedStudents should be ('empty)

		command.markerMapping = new HashMap[String, JList[String]]()
		command.markerMapping.put("cuslaj", List("cusebr", "cuscav").asJava)
		command.markerMapping.put("cuscav", List("cusxad", "cuscao", "curef").asJava)
		command.markerMapping.put("cuslat", List("cusxad", "cuscao", "curef", "cusebr").asJava)
		command.markerMapping.put("cuday", List("cuscav").asJava)
		command.applyInternal()

		assignment.markerMap.get("cuslaj").size should be(2)
		assignment.markerMap.get("cuscav").size should be(3)
		assignment.markerMap.get("cuslat").size should be(4)
		assignment.markerMap.get("cuday").size should be(1)

	} }

}
