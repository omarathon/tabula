package uk.ac.warwick.tabula.data.model

import forms.MarkerSelectField
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.{AppContextTestBase, RequestInfo}
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._

class SubmissionsForMarkerTest  extends AppContextTestBase {

	@Test def markersSubmissionsTest() {

		val assignment = new Assignment
		assignment.addDefaultFields()
		assignment.markingWorkflow = newMarkingWorkflow()

		val markerSelect = new MarkerSelectField()
		markerSelect.name = Assignment.defaultMarkerSelectorName
		assignment.addFields(markerSelect)

		val values1 = Set(
			submissionValue(Assignment.defaultCommentFieldName, "comment"),
			submissionValue(Assignment.defaultUploadName, "junk"),
			submissionValue(Assignment.defaultMarkerSelectorName, "cuslaj")
		)

		val values2 = Set(
			submissionValue(Assignment.defaultCommentFieldName, "comment"),
			submissionValue(Assignment.defaultUploadName, "junk"),
			submissionValue(Assignment.defaultMarkerSelectorName, "cuslaj")
		)

		val values3 = Set(
			submissionValue(Assignment.defaultCommentFieldName, "comment"),
			submissionValue(Assignment.defaultUploadName, "junk"),
			submissionValue(Assignment.defaultMarkerSelectorName, "cusebr")
		)

		assignment.submissions = ArrayList(
			newSubmission(assignment, values1),
			newSubmission(assignment, values2),
			newSubmission(assignment, values3)
		)
		releaseAllSubmissions(assignment)

		withUser(code = "cusebr", universityId = "0678022") {
			val user = RequestInfo.fromThread.get.user
			val submissions = assignment.getMarkersSubmissions(user.apparentUser)
			submissions.size should be (1)
		}

		withUser(code = "cuslaj", universityId = "1170836") {
			val user = RequestInfo.fromThread.get.user
			val submissions = assignment.getMarkersSubmissions(user.apparentUser)
			submissions.size should be (2)
		}

	}


	def submissionValue(name: String, value: String) = {
		val sv = new SavedSubmissionValue()
		sv.name = name
		sv.value = value
		sv
	}

	def releaseAllSubmissions(assignment: Assignment){
		assignment.submissions.foreach{s=>
			val newFeedback = new Feedback
			newFeedback.assignment = assignment
			newFeedback.uploaderId = "test"
			newFeedback.universityId = s.universityId
			newFeedback.released = false
			val markerFeedback = newFeedback.retrieveFirstMarkerFeedback
			markerFeedback.state = MarkingState.ReleasedForMarking
			assignment.feedbacks.add(newFeedback)
		}
	}

	def newMarkingWorkflow(): MarkingWorkflow = {
		val ug = new UserGroup()
		ug.includeUsers = List ("cuslaj", "cusebr")

		val ms = new MarkingWorkflow()
		ms.name = "Test marking workflow"
		ms.markingMethod = StudentsChooseMarker
		ms.firstMarkers = ug
		ms
	}

	def newSubmission(a:Assignment, values:JSet[SavedSubmissionValue]=null) = {
		val s = new Submission
		s.assignment = a
		if (values != null) s.values = values
		s
	}

}
