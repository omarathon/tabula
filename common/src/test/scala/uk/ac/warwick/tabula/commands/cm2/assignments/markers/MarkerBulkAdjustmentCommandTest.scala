package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Feedback, GradeBoundary}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{SelectedModerationCompleted, SelectedModerationMarker, SelectedModerationModerator, SingleMarker}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, ModerationSampler, SelectedModeratedWorkflow, SingleMarkerWorkflow}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


class MarkerBulkAdjustmentCommandTest extends TestBase  with Mockito {

	private trait TestFixture {

		val moderator: User = Fixtures.user("1170836", "cuslaj")
		val moderator2: User = Fixtures.user("11708365", "cuslai")
		val marker1: User = Fixtures.user("1170837", "cuslak")
		val marker2: User = Fixtures.user("1170838", "cuslal")

		val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]

		gradeGenerator.applyForMarks(any[Map[String, Int]]) answers(arg => {
			val marks = arg.asInstanceOf[Map[String, Int]]
			marks.map{case (id, mark) => (id, mark) match {
				case (i, _) if id == "3" => i -> Seq() // Mocks that ID 3 isn't linked to an assessment component in SITS with a mark scheme
				case (_, m) if m < 40 => id -> Seq(GradeBoundary(null, "F", 0, 39, "N"))
				case (_, m) if m >= 40 && mark < 50 => id -> Seq(GradeBoundary(null, "3", 40, 50, "N"))
				case (_, m) if m >= 50 && mark < 60 => id -> Seq(GradeBoundary(null, "22", 50, 60, "N"))
				case (_, m) if m >= 60 && mark < 70 => id -> Seq(GradeBoundary(null, "21", 60, 70, "N"))
				case (_, m) if m >= 70 => id -> Seq(GradeBoundary(null, "1", 70, 100, "N"))
			}}
		})


		//returns Map("0672088" -> Seq(GradeBoundary(null, "A", 0, 100, null)))

		val assignment: Assignment = newDeepAssignment()
		val workflow = SelectedModeratedWorkflow("test", assignment.module.adminDepartment, ModerationSampler.Marker, Seq(marker1, marker2), Seq(moderator))
		assignment.cm2MarkingWorkflow = workflow

		def feedbackFixture(studentId: String, mark: Option[Int], grade: Option[String], currentStage: MarkingWorkflowStage): Feedback = {
			val feedback = Fixtures.assignmentFeedback(studentId, studentId)
			assignment.feedbacks.add(feedback)
			feedback.outstandingStages.add(currentStage)

			feedback.markerFeedback.add({
				val m = Fixtures.markerFeedback(feedback)
				m.marker = marker1
				m.userLookup = Fixtures.userLookupService(marker1)
				m.stage = SelectedModerationMarker
				m.mark = mark
				m.grade = grade
				m
			})

			feedback.markerFeedback.add({
				val m = Fixtures.markerFeedback(feedback)
				m.marker = moderator
				m.userLookup = Fixtures.userLookupService(marker1)
				m.stage = SelectedModerationModerator
				m
			})

			feedback
		}

		val testData = Seq(
			("1", Some(61), Some("21"), SelectedModerationMarker), // Marking not finished
			("2", Some(63), Some("21"), SelectedModerationModerator), // Published
			("3", Some(52), Some("22"), SelectedModerationCompleted), // No grade boundary
			("4", Some(57), Some("22"), SelectedModerationModerator), // Moderated by different user
			("5", None, None, SelectedModerationCompleted), // No mark
			("6", Some(70), Some("1"), SelectedModerationModerator),
			("7", Some(42), Some("3"), SelectedModerationCompleted),
			("8", Some(39), Some("F"), SelectedModerationModerator),
			("9", Some(65), Some("21"), SelectedModerationCompleted)
		)

		val feedback: Seq[Feedback] = testData.map(d => feedbackFixture(d._1, d._2, d._3, d._4))
		feedback.find(_.usercode == "2").foreach(_.released = true)
		feedback.find(_.usercode == "4").foreach(f => {f.allMarkerFeedback.find(_.stage == SelectedModerationModerator)}.foreach(mf => {
			mf.marker = moderator2
			mf.userLookup = Fixtures.userLookupService(moderator2)
		}))

		val markerFeeback = feedback.flatMap(_.allMarkerFeedback)

		trait MockCM2MarkingWorkflowServiceComponent extends CM2MarkingWorkflowServiceComponent {
			val cm2MarkingWorkflowService: CM2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]

			cm2MarkingWorkflowService.getAllFeedbackForMarker(any[Assignment], any[User])

			cm2MarkingWorkflowService.getAllStudentsForMarker

		}

		trait MockFeedbackServiceComponent extends FeedbackServiceComponent {
			val feedbackService: FeedbackService = smartMock[FeedbackService]
		}

	}






	@Test
	def cantReleaseIfNoMarkerAssigned() = new TestFixture {
		withUser("1170836", "cuslaj") {

			val cmd = new MarkerBulkAdjustmentCommandInternal(assignment, moderator, currentUser, SelectedModerationModerator, gradeGenerator)
				with MockCM2MarkingWorkflowServiceComponent with MockFeedbackServiceComponent

		}
	}

	@Test
	def testStudentsAlreadyReleased() {
		withUser("test") {
			val marker = Fixtures.user("1170836", "cuslaj")
			val assignment = newDeepAssignment()
			assignment.cm2MarkingWorkflow = SingleMarkerWorkflow("test", assignment.module.adminDepartment, Seq(marker))

			val feedback = Fixtures.assignmentFeedback("1", "1")
			feedback.outstandingStages.add(SingleMarker)
			assignment.feedbacks.add(feedback)

			val cmd = ReleaseForMarkingCommand(assignment, currentUser.apparentUser)
			cmd.students = Seq("1", "2", "3").asJava
			// no known marker as firstmarker not assigned
			cmd.studentsWithoutKnownMarkers should be(Seq("1","2","3"))
			feedback.markerFeedback.add({
				val m = Fixtures.markerFeedback(feedback)
				val u = Fixtures.user("marker", "marker")
				m.marker = u
				m.userLookup = Fixtures.userLookupService(u)
				m.stage = SingleMarker
				m
			})
			// has known marker as first marker is assigned
			cmd.studentsWithoutKnownMarkers should be(Seq("2","3"))
			cmd.studentsAlreadyReleased should be(Seq("1"))
			cmd.unreleasableSubmissions should be(Seq("2","3","1"))
		}
	}

}