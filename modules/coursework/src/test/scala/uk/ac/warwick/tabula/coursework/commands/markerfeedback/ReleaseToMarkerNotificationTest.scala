package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.data.model.{Notification, ModeratedMarkingWorkflow, Feedback, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.coursework.MockRenderer
import uk.ac.warwick.tabula.data.model.notifications.ReleaseToMarkerNotification

class ReleaseToMarkerNotificationTest  extends TestBase with Mockito {

	val TEST_CONTENT = "test"

	def createNotification(agent: User, recipient: User, _object: Seq[MarkerFeedback], assignment: Assignment, isFirstMarker: Boolean) = {
		val n = Notification.init(new ReleaseToMarkerNotification, agent, _object, assignment)
		n.whichMarker.value = if (isFirstMarker) 1 else 2
		n
	}

	trait ReleaseNotificationFixture extends MarkingNotificationFixture {

		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow

		val (f1, mf1) = makeMarkerFeedback(student1)(MarkingNotificationFixture.FirstMarkerLink)
		val (f2, mf2) = makeMarkerFeedback(student2)(MarkingNotificationFixture.FirstMarkerLink)
	}

	@Test
	def titleIncludesModuleAndAssignmentName(){ new ReleaseNotificationFixture {
		val n =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)
		n.title should be("Feedback released for HERON101 - Test assignment")
	} }

	@Test
	def urlIsProfilePageForStudents():Unit = new ReleaseNotificationFixture{
		val n =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)
		n.url should be("/admin/module/heron101/assignments/1/marker/list")
	}


	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ReleaseNotificationFixture {
		val n =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)

		val content = n.content
		content.template should be ("/WEB-INF/freemarker/emails/released_to_marker_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ReleaseNotificationFixture {
		val n =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)

		val model = n.content.model

		model.get("markingUrl") should be(Some(n.url))
		model.get("assignment") should be(Some(testAssignment))
		model.get("numReleasedFeedbacks") should be(Some(2))
		model.get("verb") should be(Some(Some("mark")))
	}
}