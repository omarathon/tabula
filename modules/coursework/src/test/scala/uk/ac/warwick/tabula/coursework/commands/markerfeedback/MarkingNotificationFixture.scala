package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.{Mockito, Fixtures}
import uk.ac.warwick.tabula.services.UserLookupService
import org.mockito.Mockito._
import uk.ac.warwick.tabula.data.model.{Department, Module, MarkerFeedback, Assignment, Feedback, UserGroup}
import uk.ac.warwick.userlookup.User

trait MarkingNotificationFixture extends Mockito {

	val department = new Department()
	val testmodule = new Module("heron101", department)
	val testAssignment = new Assignment{ id = "1"; name = "Test assignment"; module = testmodule }

	val marker1 = Fixtures.user("marker1", "marker1")
	val marker2 = Fixtures.user("marker2", "marker2")
	val marker3 = Fixtures.user("marker3", "marker3")
	val student1 = Fixtures.user("student1", "student1")
	val student2 = Fixtures.user("student2", "student2")
	val student3 = Fixtures.user("student3", "student3")
	val student4 = Fixtures.user("student4", "student4")

	val mockUserLookup = mock[UserLookupService]
	when(mockUserLookup.getUserByUserId("marker1")) thenReturn marker1
	when(mockUserLookup.getUserByUserId("marker2")) thenReturn marker2
	when(mockUserLookup.getUserByUserId("marker3")) thenReturn marker3
	when(mockUserLookup.getUserByWarwickUniId("student1")) thenReturn student1
	when(mockUserLookup.getUserByWarwickUniId("student2")) thenReturn student2
	when(mockUserLookup.getUserByWarwickUniId("student3")) thenReturn student3
	when(mockUserLookup.getUserByWarwickUniId("student4")) thenReturn student4

	def userGroup(usercodes: String *) = {
		val userGroup = UserGroup.ofUsercodes
		userGroup.includeUsers = usercodes
		userGroup
	}

	def makeMarkerFeedback(student: User)(linkFunction: (Feedback, MarkerFeedback) => Unit) = {
		val feedback = new Feedback()
		feedback.universityId = student.getWarwickId
		val mf = new MarkerFeedback()
		mf.feedback = feedback
		feedback.secondMarkerFeedback = mf
		feedback.assignment = testAssignment
		testAssignment.feedbacks.add(feedback)
		// link the markerFeedback to the appropriate field of the feedback
		linkFunction(feedback, mf)
		(feedback, mf)
	}
}
