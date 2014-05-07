package uk.ac.warwick.tabula.coursework.commands.markerfeedback


import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{MockUserLookup, TestFixtures}
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import uk.ac.warwick.userlookup.User

// reusable environment for marking workflow tests
trait MarkingWorkflowWorld extends TestFixtures {

	val mockUserLookup = new MockUserLookup

	mockUserLookup.registerUserObjects(
		new User("cusxad") { setWarwickId("9876004"); setFoundUser(true); setVerified(true); },
		new User("cuscao") { setWarwickId("0270954"); setFoundUser(true); setVerified(true); },
		new User("curef") { setWarwickId("9170726"); setFoundUser(true); setVerified(true); },
		new User("cusebr") { setWarwickId("0672088"); setFoundUser(true); setVerified(true); },
		new User("cuscav") { setWarwickId("0672089"); setFoundUser(true); setVerified(true); }
	)

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

	val firstMarkers: JList[FirstMarkersMap] = JArrayList()
	firstMarkers.addAll(Seq(
		FirstMarkersMap(assignment, "cuslaj", makeUserGroup("cusxad", "cuscao", "curef")),
		FirstMarkersMap(assignment, "cuscav", makeUserGroup("cusebr", "cuscav"))
	).asJava)

	assignment.firstMarkers = firstMarkers

	val secondMarkers: JList[SecondMarkersMap] = JArrayList()
	secondMarkers.addAll(Seq(
		SecondMarkersMap(assignment, "cuslat", makeUserGroup("cusxad", "cuscao", "curef")),
		SecondMarkersMap(assignment, "cuday", makeUserGroup("cusebr", "cuscav"))
	).asJava)

	assignment.secondMarkers = secondMarkers

	def addFeedback(assignment:Assignment){
		val feedback = assignment.submissions.asScala.map{ s =>
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = "cuslaj"
			newFeedback.universityId = s.universityId
			newFeedback.released = false
			addMarkerFeedback(newFeedback,FirstFeedback)
			newFeedback
		}
		assignment.feedbacks = feedback.asJava
	}

	def setFirstMarkerFeedbackState(state: MarkingState) =
		assignment.feedbacks.asScala.foreach( mf => mf.firstMarkerFeedback.state = state )

	def setSecondMarkerFeedbackState(state: MarkingState) =
		assignment.feedbacks.asScala.foreach( mf => mf.secondMarkerFeedback.state = state )

	def setFinalMarkerFeedbackState(state: MarkingState) =
		assignment.feedbacks.asScala.foreach( mf => mf.thirdMarkerFeedback.state = state )

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
