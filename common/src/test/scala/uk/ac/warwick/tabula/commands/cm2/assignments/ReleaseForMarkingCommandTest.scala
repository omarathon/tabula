package uk.ac.warwick.tabula.commands.cm2.assignments


import uk.ac.warwick.tabula.data.model.AssignmentFeedback
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.SingleMarker
import uk.ac.warwick.tabula.data.model.markingworkflow.SingleMarkerWorkflow
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, CM2MarkingWorkflowServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

import scala.collection.JavaConverters._


class ReleaseForMarkingCommandTest extends TestBase  with Mockito {

	trait MockCM2MarkingWorkflowServiceComponent extends CM2MarkingWorkflowServiceComponent {
		val cm2MarkingWorkflowService: CM2MarkingWorkflowService = mock[CM2MarkingWorkflowService]
		cm2MarkingWorkflowService.releaseForMarking(any[Seq[AssignmentFeedback]]) answers  { f =>
			val released = f.asInstanceOf[Seq[AssignmentFeedback]]
			released.foreach(_.outstandingStages.add(SingleMarker))
			released
		}
	}

	@Test
	def cantReleaseIfNoMarkerAssigned() {
		withUser("test") {
			val assignment = newDeepAssignment()
			val marker = Fixtures.user("1170836", "cuslaj")
			val workflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))
			val cmd = ReleaseForMarkingCommand(assignment, workflow, currentUser)
			cmd.students = Seq("1", "2", "3").asJava
			cmd.unreleasableSubmissions should be(Seq("1","2","3"))
		}
	}

	@Test
	def testStudentsAlreadyReleased() {
		withUser("test") {
			val assignment = newDeepAssignment()
			val feedback = Fixtures.assignmentFeedback("1", "1")
			feedback.outstandingStages.add(SingleMarker)
			assignment.feedbacks.add(feedback)
			val marker = Fixtures.user("1170836", "cuslaj")
			val workflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))
			val cmd = ReleaseForMarkingCommand(assignment, workflow, currentUser)

			cmd.students = Seq("1", "2", "3").asJava
			cmd.studentsWithoutKnownMarkers should be(Seq("2","3"))
			cmd.studentsAlreadyReleased should be(Seq("1"))
			cmd.unreleasableSubmissions should be(Seq("2","3","1"))
		}
	}

	@Test
	def testCanReleaseIfMarkerIsAssigned() {
		withUser("test") {
			val marker = Fixtures.user("1170836", "cuslaj")
			val assignment = newDeepAssignment()
			val feedback = Fixtures.assignmentFeedback("1", "1")
			val mf = Fixtures.markerFeedback(feedback)
			mf.userLookup = Fixtures.userLookupService(marker)
			mf.marker = marker
			mf.stage = SingleMarker
			feedback.markerFeedback.add(mf)
			assignment.feedbacks.add(feedback)

			val workflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))
			val cmd = new ReleaseForMarkingCommandInternal(assignment, workflow, currentUser) with MockCM2MarkingWorkflowServiceComponent

			cmd.students = Seq("1", "2", "3").asJava
			cmd.studentsWithoutKnownMarkers should be(Seq("2","3"))
			cmd.studentsAlreadyReleased should be(Nil)
			cmd.unreleasableSubmissions should be(Seq("2","3"))

			cmd.applyInternal() should be (Seq(feedback))
			cmd.newReleasedFeedback.asScala should be (Seq(mf))
		}
	}

}