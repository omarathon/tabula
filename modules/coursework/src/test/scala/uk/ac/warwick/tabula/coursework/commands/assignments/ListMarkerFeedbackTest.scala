package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, AppContextTestBase}
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model._
import java.util.HashMap


class ListMarkerFeedbackTest extends AppContextTestBase with Mockito {

	@Transactional @Test
	def firstMarkerTest {
		new MarkingCycleWorld {
			withUser("cuslaj") {
				val command =
					new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser, assignment.isFirstMarker(currentUser.apparentUser))
				val markerFeedbackItems = command.apply()

				command.completedFeedback.size should be (0)
				markerFeedbackItems.size should be (3)
			}
			withUser("cuscav") {
				val command =
					new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser, assignment.isFirstMarker(currentUser.apparentUser))
				val markerFeedbackItems = command.apply()

				command.completedFeedback.size should be (0)
				markerFeedbackItems.size should be (2)
			}
		}
	}

	@Transactional @Test
	def secondMarkerTest {
		new MarkingCycleWorld {

			assignment.feedbacks.foreach{feedback =>
				val smFeedback = feedback.retrieveSecondMarkerFeedback
				smFeedback.state = ReleasedForMarking
			}

			withUser("cuslat") {
				val command =
					new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser, assignment.isFirstMarker(currentUser.apparentUser))
				val markerFeedbackItems = command.apply()

				command.completedFeedback.size should be (0)
				markerFeedbackItems.size should be (3)
			}
			withUser("cuday") {
				val command =
					new ListMarkerFeedbackCommand(assignment, assignment.module, currentUser, assignment.isFirstMarker(currentUser.apparentUser))
				val markerFeedbackItems = command.apply()

				command.completedFeedback.size should be (0)
				markerFeedbackItems.size should be (2)
			}
		}
	}

	// reusable environment
	trait MarkingCycleWorld {

		val assignment = newDeepAssignment(moduleCode = "IN101")
		generateSubmission(assignment, "9876004", "cusxad")
		generateSubmission(assignment, "0270954", "cuscao")
		generateSubmission(assignment, "9170726", "curef")
		generateSubmission(assignment, "0672088", "cusebr")
		generateSubmission(assignment, "0672089", "cuscav")
		addFeedback(assignment)

		val markScheme = new MarkScheme()
		markScheme.markingMethod = SeenSecondMarking
		markScheme.department = assignment.module.department
		markScheme.firstMarkers = makeUserGroup("cuslaj", "cuscav")
		markScheme.secondMarkers = makeUserGroup("cuslat", "cuday")
		assignment.markScheme = markScheme

		assignment.markerMap = new HashMap[String, UserGroup]()
		assignment.markerMap.put("cuslaj", makeUserGroup("cusxad", "cuscao", "curef"))
		assignment.markerMap.put("cuscav", makeUserGroup("cusebr", "cuscav"))
		assignment.markerMap.put("cuslat", makeUserGroup("cusxad", "cuscao", "curef"))
		assignment.markerMap.put("cuday", makeUserGroup("cusebr", "cuscav"))

		def addFeedback(assignment:Assignment){
			val feedback = assignment.submissions.map{s=>
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = "cuslaj"
				newFeedback.universityId = s.universityId
				newFeedback.released = false
				val fmFeedback = newFeedback.retrieveFirstMarkerFeedback
				fmFeedback.state = ReleasedForMarking
				newFeedback
			}
			assignment.feedbacks = feedback
		}


		def generateSubmission(assignment:Assignment, uniId: String, userCode: String) {
			val submission = new Submission()
			submission.assignment = assignment
			submission.universityId = uniId
			submission.userId = userCode
			assignment.submissions.add(submission)
		}

		def makeUserGroup(users: String*): UserGroup = {
			val ug = new UserGroup
			ug.includeUsers = users
			ug
		}
	}
}


