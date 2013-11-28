package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{Feedback, UserGroup, Assignment, Module}
import scala.collection.JavaConverters._
import java.util
import uk.ac.warwick.tabula.helpers.Tap.tap
import uk.ac.warwick.tabula.services.{FeedbackServiceComponent, StateServiceComponent, AssignmentServiceComponent, AssignmentService, FeedbackService, StateService}

class ReleaseForMarkingCommandTest extends TestBase  with Mockito {

	val ug1 = UserGroup.ofUniversityIds.tap(g=>{
		g.includeUsers = Seq("1", "2","4").asJava
		g.userLookup = new MockUserLookup(true)
	})
	val ug2 = UserGroup.ofUniversityIds.tap(g=>{
		g.includeUsers = Seq("1", "2","3").asJava
		g.userLookup = new MockUserLookup(true)
	})

	@Test
	def cantReleaseIfNoMarkerAssigned() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.markerMap = new util.HashMap[String, UserGroup]()
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq()
			}

			cmd.students = Seq("1", "2", "3").asJava
			cmd.unreleasableSubmissions should be(Seq("1","2","3"))
			cmd.studentsWithKnownMarkers should be(Seq())
		}
	}

	@Test
	def testCanReleaseIfMarkerIsAssigned() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.markerMap = Map("marker1" -> ug1).asJava
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq("1","2")
			}

			cmd.students = Seq("1", "2", "3").asJava
			cmd.unreleasableSubmissions should be(Seq("3"))
			cmd.studentsWithKnownMarkers should be(Seq("1","2"))
		}
	}
	@Test
	def releasesAllSubmissionsIfMarkersAllocated() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.markerMap = Map("marker1" -> ug2).asJava
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq("1","2", "3")
			}
			cmd.students = Seq("1", "2", "3").asJava

			val feedbacks = cmd.applyInternal()
			feedbacks.length should be (3)
		}
	}

	@Test
	def releasesOnlyReleaseableSubmissions() {
		withUser("test") {

			val assignment = new Assignment().tap {
				a =>
					a.markerMap = Map("marker1" -> ug1).asJava
					a.module = new Module().tap(_.id = "module_id")
			}

			val cmd = new ReleaseForMarkingCommand(assignment.module, assignment, currentUser.apparentUser)
			with ReleaseForMarkingCommandTestSupport {
				override def studentsWithKnownMarkers = Seq("1","2")
			}
			cmd.students = Seq("1", "2", "3").asJava

			val feedbacks = cmd.applyInternal()
			feedbacks.length should be (2)
		}
	}
}

trait ReleaseForMarkingCommandTestSupport extends AssignmentServiceComponent with StateServiceComponent
with FeedbackServiceComponent with Mockito {

	val assignmentService = mock[AssignmentService]
	val stateService = mock[StateService]
	val feedbackService = mock[FeedbackService]
	def apply(): List[Feedback] = List()
}
