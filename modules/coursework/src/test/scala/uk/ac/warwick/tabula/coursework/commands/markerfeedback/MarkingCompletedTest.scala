package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.commands.assignments.MarkingCompletedCommand
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import org.springframework.transaction.annotation.Transactional
import org.hibernate.Session
import uk.ac.warwick.tabula.data.SessionComponent


class MarkingCompletedTest extends AppContextTestBase with MarkingWorkflowWorld with Mockito {

	val mockSession = mock[Session]
	var stateService: StateService = new ComposableStateServiceImpl with SessionComponent {
		def session = mockSession
	}

	@Transactional @Test
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

			command.apply()
			val releasedFeedback = assignment.feedbacks.map(_.firstMarkerFeedback).filter(_.state == MarkingState.MarkingCompleted)
			releasedFeedback.size should be (3)
		}
	}

	@Transactional @Test
	def secondMarkerFinished(){
		withUser("cuday"){
			val isFirstMarker = assignment.isFirstMarker(currentUser.apparentUser)
			val command = new MarkingCompletedCommand(assignment.module, assignment, currentUser, isFirstMarker)
			command.stateService = stateService
			command.students = List("0672088", "0672089")
			command.onBind()

			command.preSubmitValidation()
			command.noFeedback.size should be (2)
			command.noMarks.size should be (2)

			command.apply()
			val secondFeedback = assignment.feedbacks.flatMap(f => Option(f.secondMarkerFeedback))
			val releasedFeedback = secondFeedback.filter(_.state == MarkingState.MarkingCompleted)
			releasedFeedback.size should be (2)
		}
	}

}
