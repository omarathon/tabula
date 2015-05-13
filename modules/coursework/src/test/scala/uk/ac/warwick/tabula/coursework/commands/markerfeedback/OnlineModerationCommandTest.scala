package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.{Appliable, UserAware}
import uk.ac.warwick.tabula.coursework.commands.assignments.{FinaliseFeedbackCommand, FinaliseFeedbackComponent}
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected, ReleasedForMarking}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{SavedFormValueDao, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class OnlineModerationCommandTest extends TestBase with Mockito {
	trait Fixture {
		def fakeUser(id:String) = {
			val newUser = new User(id)
			newUser.setWarwickId(id)
			newUser
		}

		val student = fakeUser("user1")

		val assignment = new Assignment
		val module = new Module
		assignment.module = module
		assignment.collectMarks = true
		module.adminDepartment = new Department

		val marker = fakeUser("marker")
		val currentUser = new CurrentUser(realUser = marker, apparentUser = marker)

		val gradeGenerator = smartMock[GeneratesGradesFromMarks]
		gradeGenerator.applyForMarks(Map("user1" -> 69)) returns Map("user1" -> Seq())

		val command = new OnlineModerationCommand(module, assignment, student, currentUser.apparentUser, currentUser, gradeGenerator) with ModerationCommandSupport
			with FinaliseFeedbackTestImpl

		val testFeedback = new AssignmentFeedback
		testFeedback.universityId = "user1"
		assignment.feedbacks.add(testFeedback)
		val firstMarkerFeedback = new MarkerFeedback {
			mark = Some(69)
			grade = Some("2:1")
			feedback = testFeedback
		}
		val secondMarkerFeedback = new MarkerFeedback { feedback = testFeedback }
		testFeedback.firstMarkerFeedback = firstMarkerFeedback
		testFeedback.secondMarkerFeedback = secondMarkerFeedback

		when (command.feedbackService.getAssignmentFeedbackByUniId(assignment, "user1")) thenReturn Some(testFeedback)
	}


	@Test
	def commandApply() {
		new Fixture {
			command.approved = false
			val heronRebuke = "This feedback just rambles on about the evil nature of Herons. Please give useful feedback!"
			command.rejectionComments = heronRebuke
			command.mark = "68"
			command.grade = "2:1"
			val fs = command.feedbackService
			command.applyInternal()

			firstMarkerFeedback.state should be(Rejected)
			secondMarkerFeedback.state should be(Rejected)

			secondMarkerFeedback.rejectionComments should be(heronRebuke)
			secondMarkerFeedback.mark should be(Some(68))
			secondMarkerFeedback.grade should be(Some("2:1"))

			command.approved = true
			firstMarkerFeedback.state = ReleasedForMarking
			secondMarkerFeedback.state = ReleasedForMarking
			command.applyInternal()

			secondMarkerFeedback.state should be(MarkingCompleted)
			testFeedback.actualMark should be (Some(69))
			testFeedback.actualGrade should be (Some("2:1"))
		}
	}

	trait ModerationCommandSupport extends FeedbackServiceComponent with Appliable[MarkerFeedback]
		with FileAttachmentServiceComponent with ZipServiceComponent with MarkerFeedbackStateCopy with OnlineFeedbackState
		with OnlineFeedbackStudentState with CopyFromFormFields with WriteToFormFields with SavedFormValueDaoComponent
		with ProfileServiceComponent
	{
		def feedbackService = smartMock[FeedbackService]
		def fileAttachmentService = smartMock[FileAttachmentService]
		def zipService = smartMock[ZipService]
		def savedFormValueDao = smartMock[SavedFormValueDao]
		def profileService = smartMock[ProfileService]
		def apply() = new MarkerFeedback()
	}

	trait FinaliseFeedbackTestImpl extends FinaliseFeedbackComponent {
		self: UserAware =>

		def finaliseFeedback(assignment: Assignment, markerFeedbacks: Seq[MarkerFeedback]) {
			val finaliseFeedbackCommand = FinaliseFeedbackCommand(assignment, markerFeedbacks, user)
			finaliseFeedbackCommand.applyInternal()
		}
	}

}
