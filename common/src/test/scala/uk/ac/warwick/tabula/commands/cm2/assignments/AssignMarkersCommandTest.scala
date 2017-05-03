package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.{DescriptionImpl, ValidatorHelpers}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{ModerationMarker, ModerationModerator}
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService._
import uk.ac.warwick.tabula.services.UserLookupComponent

class AssignMarkersCommandTest extends TestBase with Mockito with ValidatorHelpers {

	trait Fixture {
		val marker1 = Fixtures.user("1170836", "cuslaj")
		val marker2 = Fixtures.user("1170837", "cuslak")
		val moderator = Fixtures.user("1170838", "cuslal")
		val student1 = Fixtures.user("1431777", "u1431777")
		val student2 = Fixtures.user("1431778", "u1431778")
		val student3 = Fixtures.user("1431779", "u1431779")
		val student4 = Fixtures.user("1431780", "u1431780")
		val student5 = Fixtures.user("1431781", "u1431781")
		val student6 = Fixtures.user("1431782", "u1431782")
		val userlookupService = Fixtures.userLookupService(marker1, marker2, moderator, student1, student2, student3, student4, student5, student6)
		val a1 = Fixtures.assignment("a1")
		a1.id = "a1"

		val alloc: Map[MarkingWorkflowStage, Allocations] = Map(
			ModerationMarker -> Map(marker1 -> Set(student1, student2, student3), marker2 -> Set(student4, student5, student6)),
			ModerationModerator -> Map(moderator -> Set(student1, student2, student3, student4, student5, student6))
		)
	}

	@Test
	def testDescription(): Unit = new Fixture {
		val d = new  DescriptionImpl
		val description = new AssignMarkersDescription with AssignMarkersState with UserLookupComponent {
			val userLookup = userlookupService
			val eventName = "AssignMarkers"
			val assignment: Assignment = a1
			override val allocationMap: Map[MarkingWorkflowStage, Allocations] = alloc
		}
		description.describe(d)
		d.allProperties("assignment") should be (a1.id)
		d.allProperties("allocations") should be ("Marker:\ncuslaj -> u1431777,u1431778,u1431779\ncuslak -> u1431780,u1431781,u1431782\nModerator:\ncuslal -> u1431777,u1431778,u1431779,u1431780,u1431781,u1431782")
	}

}
