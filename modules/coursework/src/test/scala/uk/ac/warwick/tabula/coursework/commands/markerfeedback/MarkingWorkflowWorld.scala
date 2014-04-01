package uk.ac.warwick.tabula.coursework.commands.markerfeedback


import collection.JavaConversions._

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.TestFixtures
import java.util

// reusable environment for marking workflow tests
trait MarkingWorkflowWorld extends TestFixtures {

	val assignment = newDeepAssignment(moduleCode = "IN101")
	generateSubmission(assignment, "9876004", "cusxad")
	generateSubmission(assignment, "0270954", "cuscao")
	generateSubmission(assignment, "9170726", "curef")
	generateSubmission(assignment, "0672088", "cusebr")
	generateSubmission(assignment, "0672089", "cuscav")
	addFeedback(assignment)

	var markingWorkflow = new SeenSecondMarkingWorkflow()
	markingWorkflow.department = assignment.module.department
	markingWorkflow.firstMarkers = makeUserGroup("cuslaj", "cuscav")
	markingWorkflow.secondMarkers = makeUserGroup("cuslat", "cuday")
	assignment.markingWorkflow = markingWorkflow

	assignment.markerMap = new util.HashMap[String, UserGroup]()
	assignment.markerMap.put("cuslaj", makeUserGroup("cusxad", "cuscao", "curef"))
	assignment.markerMap.put("cuscav", makeUserGroup("cusebr", "cuscav"))
	assignment.markerMap.put("cuslat", makeUserGroup("cusxad", "cuscao", "curef"))
	assignment.markerMap.put("cuday", makeUserGroup("cusebr", "cuscav"))

	def addFeedback(assignment:Assignment){
		val feedback = assignment.submissions.map{ s =>
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = "cuslaj"
			newFeedback.universityId = s.universityId
			newFeedback.released = false
			addMarkerFeedback(newFeedback,FirstFeedback)
			newFeedback
		}
		assignment.feedbacks = feedback
	}

	def setFirstMarkerFeedbackState(state: MarkingState) =
		assignment.feedbacks.foreach( mf => mf.firstMarkerFeedback.state = state )

	def setSecondMarkerFeedbackState(state: MarkingState) =
		assignment.feedbacks.foreach( mf => mf.secondMarkerFeedback.state = state )

	def setFinalMarkerFeedbackState(state: MarkingState) =
		assignment.feedbacks.foreach( mf => mf.thirdMarkerFeedback.state = state )

	def addMarkerFeedback(feedback: Feedback, position: FeedbackPosition) = {
		val mf = position match {
			case (ThirdFeedback) => feedback.retrieveThirdMarkerFeedback
			case (SecondFeedback) => feedback.retrieveSecondMarkerFeedback
			case _ => feedback.retrieveFirstMarkerFeedback
		}
		mf.state = MarkingState.InProgress
	}


	def generateSubmission(assignment:Assignment, uniId: String, userCode: String) {
		val submission = new Submission()
		submission.assignment = assignment
		submission.universityId = uniId
		submission.userId = userCode
		assignment.submissions.add(submission)
	}

	def makeUserGroup(users: String*): UserGroup = {
		val ug = UserGroup.ofUsercodes
		ug.includedUserIds = users
		ug
	}
}
