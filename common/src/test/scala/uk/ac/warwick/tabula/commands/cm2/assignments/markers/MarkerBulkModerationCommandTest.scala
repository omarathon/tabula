package uk.ac.warwick.tabula.commands.cm2.assignments.markers

import uk.ac.warwick.tabula.data.model.MarkType.Adjustment
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage.{SelectedModerationCompleted, SelectedModerationMarker, SelectedModerationModerator}
import uk.ac.warwick.tabula.data.model.markingworkflow.{MarkingWorkflowStage, ModerationSampler, SelectedModeratedWorkflow}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.SortedMap


class MarkerBulkModerationCommandTest extends TestBase  with Mockito {

	trait TestFixture {

		val moderator: User = Fixtures.user("1170836", "cuslaj")
		val moderator2: User = Fixtures.user("11708365", "cuslai")
		val marker1: User = Fixtures.user("1170837", "cuslak")
		val marker2: User = Fixtures.user("1170838", "cuslal")

		val testData: Seq[(String, Option[Int], Option[String], MarkingWorkflowStage)] = Seq(
			("1", Some(61), Some("21"), SelectedModerationMarker), // Marking not finished
			("2", Some(63), Some("21"), SelectedModerationModerator), // Published
			("3", Some(52), Some("22"), SelectedModerationCompleted), // No grade boundary
			("4", Some(57), Some("22"), SelectedModerationModerator), // Moderated by different user
			("5", None, None, SelectedModerationCompleted), // No mark
			("6", Some(70), Some("1"), SelectedModerationModerator),
			("7", Some(42), Some("3"), SelectedModerationCompleted),
			("8", Some(39), Some("F"), SelectedModerationModerator),
			("9", Some(65), Some("21"), SelectedModerationCompleted),
			("10", Some(100), Some("1"), SelectedModerationCompleted),
			("11", Some(0), Some("F"), SelectedModerationCompleted),
			("12", Some(54), Some("22"), SelectedModerationCompleted), // Has adjustments
			("13", Some(62), Some("21"), SelectedModerationCompleted), // Wasn't moderated (sent straight to admin)
		)

		val students: Seq[User] = 1 to testData.length map(i => Fixtures.user(s"$i", s"$i"))
		val mockUserLookup: UserLookupService = Fixtures.userLookupService(Seq(moderator, moderator2, marker1, marker2) ++ students: _*)

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

		val assignment: Assignment = newDeepAssignment()
		val workflow = SelectedModeratedWorkflow("test", assignment.module.adminDepartment, ModerationSampler.Marker, Seq(marker1, marker2), Seq(moderator))
		workflow.stageMarkers.asScala.foreach(_.markers.asInstanceOf[UserGroup].userLookup = mockUserLookup)
		assignment.cm2MarkingWorkflow = workflow

		def feedbackFixture(studentId: String, mark: Option[Int], grade: Option[String], currentStage: MarkingWorkflowStage): Feedback = {
			val feedback = Fixtures.assignmentFeedback(studentId, studentId)
			val student = students.find(_.getUserId == studentId).get
			assignment.feedbacks.add(feedback)
			feedback.outstandingStages.add(currentStage)


			val markerF = Fixtures.markerFeedback(feedback)
			markerF.marker = marker1
			markerF.userLookup = Fixtures.userLookupService(marker1, student)
			markerF.stage = SelectedModerationMarker
			markerF.mark = mark
			markerF.grade = grade

			val moderatorF = Fixtures.markerFeedback(feedback)
			moderatorF.marker = moderator
			moderatorF.userLookup = Fixtures.userLookupService(moderator, student)
			moderatorF.stage = SelectedModerationModerator
			moderatorF.mark = mark
			moderatorF.grade = grade

			feedback
		}

		val feedback: Seq[Feedback] = testData.map(d => feedbackFixture(d._1, d._2, d._3, d._4))
		feedback.find(_.usercode == "2").foreach(_.released = true)
		feedback.find(_.usercode == "4").foreach(f => {f.allMarkerFeedback.find(_.stage == SelectedModerationModerator)}.foreach(mf => {
			mf.marker = moderator2
			mf.userLookup = Fixtures.userLookupService(moderator2, mf.student)
		}))
		feedback.find(_.usercode == "12").foreach(_.addMark("admin", Adjustment, 0, Some("F"), "Late submission"))
		feedback.find(_.usercode == "13").foreach(f => {
			f.allMarkerFeedback.find(_.stage == SelectedModerationModerator)}.foreach(mf => {
				mf.mark = None
				mf.grade = None
			}
		))


		val markerFeedback: Seq[MarkerFeedback] = feedback.flatMap(_.allMarkerFeedback)

		trait MockCM2MarkingWorkflowServiceComponent extends CM2MarkingWorkflowServiceComponent {
			val cm2MarkingWorkflowService: CM2MarkingWorkflowService = smartMock[CM2MarkingWorkflowService]

			cm2MarkingWorkflowService.getAllFeedbackForMarker(any[Assignment], any[User]) answers(a => {
				val args = a.asInstanceOf[Array[AnyRef]]
				val user = args(1).asInstanceOf[User]
				SortedMap(markerFeedback.filter(_.marker == user).groupBy(_.stage).toSeq: _*)
			})

			cm2MarkingWorkflowService.getAllStudentsForMarker(any[Assignment], any[User]) answers(a => {
				val args = a.asInstanceOf[Array[AnyRef]]
				val user = args(1).asInstanceOf[User]

				markerFeedback.filter(_.marker == user).map(_.student)
			})

		}

		trait MockFeedbackServiceComponent extends FeedbackServiceComponent {
			val feedbackService: FeedbackService = smartMock[FeedbackService]
		}

		var populated = 0
		trait MockPopulateFeedbackComponent extends PopulateMarkerFeedbackComponent {
			def populateMarkerFeedback(assignment: Assignment, markerFeedback: Seq[MarkerFeedback]): Unit = {
				markerFeedback.foreach(mf => {
					populated = populated + 1
					mf.mark = Some(62)
					mf.grade = Some("21")
				})
			}
		}

		val cmd = new MarkerBulkModerationCommandInternal(assignment, moderator, currentUser, SelectedModerationModerator, gradeGenerator)
			with MockCM2MarkingWorkflowServiceComponent with MockFeedbackServiceComponent with MockPopulateFeedbackComponent
	}

	@Test
	def commandState() { new TestFixture {
		withUser("1170836", "cuslaj") {
			cmd.skipReasons should be (Map(
				marker1 -> Map(
					students.find(_.getWarwickId == "2").get -> List("Feedback has already been published"),
					students.find(_.getWarwickId == "3").get -> List("No appropriate mark scheme is available from SITS"),
					students.find(_.getWarwickId == "5").get -> List("Feedback has no mark", "No appropriate mark scheme is available from SITS"),
					students.find(_.getWarwickId == "12").get -> List("Post-marking adjustments have been made to this feedback")
				)
			))

			val validForAdjustment = cmd.validForAdjustment
			validForAdjustment(marker1).map(_.student.getWarwickId) should be (Seq("6", "7", "8", "9", "10", "11", "13"))
		}
	}}

	@Test
	def populateFeedback() { new TestFixture {
		cmd.previousMarker = marker1
		cmd.direction = Down
		cmd.adjustmentType = Points
		cmd.adjustment = "1"

		private val result = cmd.applyInternal()
		populated should be (1)
	}}


	@Test
	def pointDown() { new TestFixture {
		cmd.previousMarker = marker1
		cmd.direction = Down
		cmd.adjustmentType = Points
		cmd.adjustment = "1"

		private val result = cmd.applyInternal()
		verify(cmd.feedbackService, times(7)).save(any[MarkerFeedback])
		verify(cmd.cm2MarkingWorkflowService, times(5)).finaliseFeedback(any[MarkerFeedback])
		private val mf6 = result.find(_.student.getWarwickId == "6").get
		mf6.mark should be (Some(69))
		mf6.grade should be (Some("21"))
		private val mf7 = result.find(_.student.getWarwickId == "7").get
		mf7.mark should be (Some(41))
		mf7.grade should be (Some("3"))
		private val mf8 = result.find(_.student.getWarwickId == "8").get
		mf8.mark should be (Some(38))
		mf8.grade should be (Some("F"))
		private val mf9 = result.find(_.student.getWarwickId == "9").get
		mf9.mark should be (Some(64))
		mf9.grade should be (Some("21"))
		private val mf11 = result.find(_.student.getWarwickId == "11").get
		mf11.mark should be (Some(0))
		mf11.grade should be (Some("F"))
	}}


	@Test
	def pointUp() { new TestFixture {
		cmd.previousMarker = marker1
		cmd.direction = Up
		cmd.adjustmentType = Points
		cmd.adjustment = "2"

		private val result = cmd.applyInternal()
		private val mf6 = result.find(_.student.getWarwickId == "6").get
		mf6.mark should be (Some(72))
		mf6.grade should be (Some("1"))
		private val mf7 = result.find(_.student.getWarwickId == "7").get
		mf7.mark should be (Some(44))
		mf7.grade should be (Some("3"))
		private val mf8 = result.find(_.student.getWarwickId == "8").get
		mf8.mark should be (Some(41))
		mf8.grade should be (Some("3"))
		private val mf9 = result.find(_.student.getWarwickId == "9").get
		mf9.mark should be (Some(67))
		mf9.grade should be (Some("21"))
		private val mf10 = result.find(_.student.getWarwickId == "10").get
		mf10.mark should be (Some(100))
		mf10.grade should be (Some("1"))
	}}

	@Test
	def percentDown() { new TestFixture {
		cmd.previousMarker = marker1
		cmd.direction = Down
		cmd.adjustmentType = Percentage
		cmd.adjustment = "5"

		private val result = cmd.applyInternal()
		private val mf6 = result.find(_.student.getWarwickId == "6").get
		mf6.mark should be (Some(67))
		mf6.grade should be (Some("21"))
		private val mf7 = result.find(_.student.getWarwickId == "7").get
		mf7.mark should be (Some(40))
		mf7.grade should be (Some("3"))
		private val mf8 = result.find(_.student.getWarwickId == "8").get
		mf8.mark should be (Some(38))
		mf8.grade should be (Some("F"))
		private val mf9 = result.find(_.student.getWarwickId == "9").get
		mf9.mark should be (Some(62))
		mf9.grade should be (Some("21"))

		private val mf10 = result.find(_.student.getWarwickId == "10").get
		mf10.mark should be (Some(95))
		mf10.grade should be (Some("1"))
		private val mf11 = result.find(_.student.getWarwickId == "11").get
		mf11.mark should be (Some(0))
		mf11.grade should be (Some("F"))
	}}

	@Test
	def percentUp() { new TestFixture {
		cmd.previousMarker = marker1
		cmd.direction = Up
		cmd.adjustmentType = Percentage
		cmd.adjustment = "2.75"

		private val result = cmd.applyInternal()
		private val mf6 = result.find(_.student.getWarwickId == "6").get
		mf6.mark should be (Some(72))
		mf6.grade should be (Some("1"))
		private val mf7 = result.find(_.student.getWarwickId == "7").get
		mf7.mark should be (Some(44))
		mf7.grade should be (Some("3"))
		private val mf8 = result.find(_.student.getWarwickId == "8").get
		mf8.mark should be (Some(41))
		mf8.grade should be (Some("3"))
		private val mf9 = result.find(_.student.getWarwickId == "9").get
		mf9.mark should be (Some(67))
		mf9.grade should be (Some("21"))

		private val mf10 = result.find(_.student.getWarwickId == "10").get
		mf10.mark should be (Some(100))
		mf10.grade should be (Some("1"))
		private val mf11 = result.find(_.student.getWarwickId == "11").get
		mf11.mark should be (Some(0))
		mf11.grade should be (Some("F"))
	}}


}