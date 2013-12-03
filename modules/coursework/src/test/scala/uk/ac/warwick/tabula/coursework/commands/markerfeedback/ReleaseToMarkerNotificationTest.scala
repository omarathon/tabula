package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.data.model.{ModeratedMarkingWorkflow, Feedback, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.ReleaseToMarkerNotifcation
import uk.ac.warwick.tabula.coursework.MockRenderer

class ReleaseToMarkerNotificationTest  extends TestBase with Mockito {

	val TEST_CONTENT = "test"

	def createNotification(agent: User, recipient: User, _object: Seq[MarkerFeedback], assignment: Assignment, isFirstMarker: Boolean) = {
		val n = new ReleaseToMarkerNotifcation(agent, recipient, _object, assignment, isFirstMarker) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	trait ReleaseNotificationFixture extends MarkingNotificationFixture {

		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow

		val linkFunction: (Feedback, MarkerFeedback) => Unit = (f,mf) => () // do nothing
		val (f1, mf1) = makeMarkerFeedback(student1)(linkFunction)
		val (f2, mf2) = makeMarkerFeedback(student2)(linkFunction)
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

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			Matchers.eq("/WEB-INF/freemarker/emails/released_to_marker_notification.ftl"),
			any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ReleaseNotificationFixture {
		val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
		val n =  createNotification(marker1, marker2, Seq(mf1, mf2), testAssignment, isFirstMarker = true)

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			any[String],
			model.capture())

		model.getValue.get("markingUrl") should be(Some(n.url))
		model.getValue.get("assignment") should be(Some(testAssignment))
		model.getValue.get("numReleasedFeedbacks") should be(Some(2))
		model.getValue.get("verb") should be(Some(Some("mark")))
	}
}