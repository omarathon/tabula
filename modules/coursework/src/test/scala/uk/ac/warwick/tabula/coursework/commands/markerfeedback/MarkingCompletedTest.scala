package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import uk.ac.warwick.tabula.data.model.notifications.coursework.ReleaseToMarkerNotification

import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.commands.assignments.{MarkingCompletedState, SecondMarkerReleaseNotifier, MarkingCompletedCommand}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import org.hibernate.Session
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.spring.Wire
import org.junit.{Before, After}
import uk.ac.warwick.tabula.events.EventListener
import uk.ac.warwick.tabula.commands.UserAware
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.ReleasedState
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.Mockito

/*
 * Fixed this test by replacing the full appcontext with a minimal functional one as below.
 * Not completely convinced this is the best solution, but as the command creates subcommands
 * and calls their apply() it seems necessary to make the services it looks for available. So
 * this may be the best we can do without refactoring Command some more.
 *
 * MinimalCommandDependencies could be pulled out and reused in other tests, though as I say,
 *
 */

object MarkingCompletedTest {
	class Ctx extends FunctionalContext with Mockito with MinimalCommandDependencies

	trait MinimalCommandDependencies extends FunctionalContext with Mockito {
		delayedInit {
			singleton() { mock[MaintenanceModeService] }
			singleton() { mock[EventListener] }
		}
	}
}

class MarkingCompletedTest extends TestBase with MarkingWorkflowWorld with Mockito with FunctionalContextTesting with FeedbackServiceComponent {

	val mockSession = mock[Session]
	var stateService: StateService = new ComposableStateServiceImpl with SessionComponent {
		def session = mockSession
	}

	var feedbackService: FeedbackService = mock[FeedbackService]

	@Before
	def before() {
		Wire.ignoreMissingBeans = true
	}

	@After
	def after() {
		Wire.ignoreMissingBeans = false
	}

	@Test
	def firstMarkerFinished() {
		withUser("cuslaj") {

			val command = MarkingCompletedCommand(assignment.module, assignment, currentUser.apparentUser, currentUser)
			command.stateService = stateService
			command.students = List("9876004", "0270954", "9170726")
			command.onBind(null)

			command.preSubmitValidation()
			command.noFeedback.size should be (3)
			command.noMarks.size should be (3)

			command.feedbackService = feedbackService

			command.applyInternal()
			val releasedFeedback = assignment.feedbacks.map(_.firstMarkerFeedback).filter(_.state == MarkingState.MarkingCompleted)
			releasedFeedback.size should be (3)

			val secondMarkerFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback)).filter(_.state == MarkingState.ReleasedForMarking)
			secondMarkerFeedback.size should be (3)

		}
	}



	@Test
	def secondMarkerFinished(){
		inContext[MarkingCompletedTest.Ctx] {
		withUser("cuday"){

			val command = MarkingCompletedCommand(assignment.module, assignment, currentUser.apparentUser, currentUser)
			command.stateService = stateService
			command.students = List("0672088", "0672089")
			setFirstMarkerFeedbackState(MarkingState.MarkingCompleted)
			command.onBind(null)

			command.preSubmitValidation()
			command.noFeedback.size should be (2)
			command.noMarks.size should be (2)

			command.feedbackService = feedbackService

			command.applyInternal()

			val firstFeedback = assignment.feedbacks.flatMap(f => Option(f.firstMarkerFeedback))
			val completedFirstMarking = firstFeedback.filter(_.state == MarkingState.MarkingCompleted)
			completedFirstMarking.size should be (5)

			val secondFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback))
			val completedSecondMarking = secondFeedback.filter(_.state == MarkingState.MarkingCompleted)
			completedSecondMarking.size should be (2)

			val finalFeedback = assignment.feedbacks.flatMap(f => Option(f.thirdMarkerFeedback)).filter(_.state == MarkingState.ReleasedForMarking)
			finalFeedback.size should be (2)


		}
		}
	}


	@Test
	def finalMarkingComplete() {
		inContext[MarkingCompletedTest.Ctx] {
			withUser("cuslaj") {

				val command = MarkingCompletedCommand(assignment.module, assignment, currentUser.apparentUser, currentUser)

				assignment.feedbacks.map(addMarkerFeedback(_, ThirdFeedback))
				assignment.feedbacks.map(addMarkerFeedback(_, SecondFeedback))
				setFinalMarkerFeedbackState(MarkingState.InProgress)
				setFirstMarkerFeedbackState(MarkingState.MarkingCompleted)
				setSecondMarkerFeedbackState(MarkingState.MarkingCompleted)

				command.stateService = stateService
				command.students = List("9876004", "0270954", "9170726")
				command.onBind(null)

				command.feedbackService = feedbackService

				command.preSubmitValidation()
				command.noFeedback.size should be(3)
				command.noMarks.size should be(3)

				command.applyInternal()

				val firstFeedback = assignment.feedbacks.flatMap(f => Option(f.firstMarkerFeedback))
				val completedFirstMarking = firstFeedback.filter(_.state == MarkingState.MarkingCompleted)
				completedFirstMarking.size should be(5)

				val secondFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback))
				val completedSecondMarking = secondFeedback.filter(_.state == MarkingState.MarkingCompleted)
				completedSecondMarking.size should be(5)

				val releasedFeedback = assignment.feedbacks.map(_.thirdMarkerFeedback).filter(_.state == MarkingState.MarkingCompleted)
				releasedFeedback.size should be(3)
			}

		}
	}


	@Test
	def notifiesEachAffectedUser() { new MarkingNotificationFixture {

		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow {
			userLookup = mockUserLookup
			firstMarkers = userGroup("marker1")
			secondMarkers = userGroup("marker2", "marker3")
		}

		val m2UserGroup = userGroup("student1", "student2")
		val m3UserGroup = userGroup("student3", "student4")

		testAssignment.secondMarkers = Seq(
			SecondMarkersMap(testAssignment, "marker2", m2UserGroup),
			SecondMarkersMap(testAssignment, "marker3", m3UserGroup)
		)

		val (f1, mf1) = makeMarkerFeedback(student1)(MarkingNotificationFixture.SecondMarkerLink)
		val (f2, mf2) = makeMarkerFeedback(student2)(MarkingNotificationFixture.SecondMarkerLink)
		val (f3, mf3) = makeMarkerFeedback(student3)(MarkingNotificationFixture.SecondMarkerLink)
		val (f4, mf4) = makeMarkerFeedback(student4)(MarkingNotificationFixture.SecondMarkerLink)

		val notifier = new SecondMarkerReleaseNotifier with MarkingCompletedState with ReleasedState with UserAware with UserLookupComponent with Logging {
			val user = marker1
			val submitter = new CurrentUser(marker1, marker1)
			val assignment = testAssignment
			val module = new Module
			newReleasedFeedback = List(mf1, mf2, mf3, mf4)
			var userLookup = mockUserLookup
		}

		val notifications = notifier.emit()
		notifications.foreach {
			case n:ReleaseToMarkerNotification => n.userLookup = mockUserLookup
		}

		notifications.size should be(2)
		notifications(0).recipients should contain(marker3)
		notifications(0).entities should contain(mf3)
		notifications(0).entities should contain(mf4)
		notifications(1).recipients should contain(marker2)
		notifications(1).entities should contain(mf1)
		notifications(1).entities should contain(mf2)

	}}



}
