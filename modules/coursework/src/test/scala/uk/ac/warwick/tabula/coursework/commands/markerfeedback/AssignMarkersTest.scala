package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import java.util.HashMap

import collection.JavaConversions._

import org.hibernate.Session
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.{Mockito, TestBase}
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
		command.userLookup = smartMock[UserLookupService]
		command.assignmentService = smartMock[AssignmentService]
		command.assignmentMembershipService = smartMock[AssignmentMembershipService]

		// mock expectations
		command.assignmentMembershipService.determineMembershipUsers(assignment) returns (Nil)
		// end expectations

		command.onBind()

		command.firstMarkers.size should be (2)
		command.secondMarkers.size should be (2)

		command.firstMarkers.find(_.userCode == "cuslaj").get.students.size should  be (3)
		command.firstMarkers.find(_.userCode == "cuscav").get.students.size should  be (2)
		command.secondMarkers.find(_.userCode == "cuslat").get.students.size should  be (3)
		command.secondMarkers.find(_.userCode == "cuday").get.students.size should  be (2)


		// students without a marker should be empty
		command.firstMarkerUnassignedStudents should be ('empty)
		command.secondMarkerUnassignedStudents should be ('empty)

		command.markerMapping = new HashMap[String, JList[String]]()
		command.markerMapping.put("cuslaj", List("cusebr", "cuscav"))
		command.markerMapping.put("cuscav", List("cusxad", "cuscao", "curef"))
		command.markerMapping.put("cuslat", List("cusxad", "cuscao", "curef", "cusebr"))
		command.markerMapping.put("cuday", List("cuscav"))
		command.applyInternal()

		assignment.markerMap.get("cuslaj").includeUsers.size should be(2)
		assignment.markerMap.get("cuscav").includeUsers.size should be(3)
		assignment.markerMap.get("cuslat").includeUsers.size should be(4)
		assignment.markerMap.get("cuday").includeUsers.size should be(1)

	} }

}
