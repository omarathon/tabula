package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkingCompletedCommand
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import org.hibernate.Session
import uk.ac.warwick.tabula.data.SessionComponent
import uk.ac.warwick.spring.Wire
import org.junit.{Before, After}
import uk.ac.warwick.tabula.events.EventListener

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

class MarkingCompletedTest extends TestBase with MarkingWorkflowWorld with Mockito with FunctionalContextTesting {

	val mockSession = mock[Session]
	var stateService: StateService = new ComposableStateServiceImpl with SessionComponent {
		def session = mockSession
	}

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
			val isFirstMarker = assignment.isFirstMarker(currentUser.apparentUser)
			val command = new MarkingCompletedCommand(assignment.module, assignment, currentUser, isFirstMarker)
			command.stateService = stateService
			command.students = List("9876004", "0270954", "9170726")
			command.onBind()

			command.preSubmitValidation()
			command.noFeedback.size should be (3)
			command.noMarks.size should be (3)

			command.applyInternal()
			val releasedFeedback = assignment.feedbacks.map(_.firstMarkerFeedback).filter(_.state == MarkingState.MarkingCompleted)
			releasedFeedback.size should be (3)
		}
	}

	@Test
	def secondMarkerFinished(){
		inContext[MarkingCompletedTest.Ctx] {
		withUser("cuday"){
			val isFirstMarker = assignment.isFirstMarker(currentUser.apparentUser)
			val command = new MarkingCompletedCommand(assignment.module, assignment, currentUser, isFirstMarker)
			command.stateService = stateService
			command.students = List("0672088", "0672089")
			command.onBind()

			command.preSubmitValidation()
			command.noFeedback.size should be (2)
			command.noMarks.size should be (2)

			command.applyInternal()
			val secondFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback))
			val releasedFeedback = secondFeedback.filter(_.state == MarkingState.MarkingCompleted)
			releasedFeedback.size should be (2)
		}
		}
	}

}
