package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.coursework.commands.assignments.AssignMarkersCommand
import java.util.HashMap
import org.springframework.transaction.annotation.Transactional


class AssignMarkersTest extends AppContextTestBase with MarkingWorkflowWorld {

	@Transactional @Test
	def assignMarkers(){
		val command = new AssignMarkersCommand(assignment.module, assignment)
		command.onBind()

		command.firstMarkers.size should be (2)
		command.secondMarkers.size should be (2)
		command.markerMapping.get("cuslaj").size should  be (3)
		command.markerMapping.get("cuscav").size should  be (2)
		command.markerMapping.get("cuslat").size should  be (3)
		command.markerMapping.get("cuday").size should  be (2)
		// students without a marker should be empty
		command.firstMarkerStudents should be ('empty)
		command.secondMarkerStudents should be ('empty)

		command.markerMapping = new HashMap[String, JList[String]]()
		command.markerMapping.put("cuslaj", List("cusebr", "cuscav"))
		command.markerMapping.put("cuscav", List("cusxad", "cuscao", "curef"))
		command.markerMapping.put("cuslat", List("cusxad", "cuscao", "curef", "cusebr"))
		command.markerMapping.put("cuday", List("cuscav"))
		command.apply()

		assignment.markerMap.get("cuslaj").includeUsers.size should be(2)
		assignment.markerMap.get("cuscav").includeUsers.size should be(3)
		assignment.markerMap.get("cuslat").includeUsers.size should be(4)
		assignment.markerMap.get("cuday").includeUsers.size should be(1)

	}

}
