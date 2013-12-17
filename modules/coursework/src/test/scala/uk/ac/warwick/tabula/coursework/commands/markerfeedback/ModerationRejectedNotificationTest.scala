package uk.ac.warwick.tabula.coursework.commands.markerfeedback

import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{ModeratorRejectedNotification, ReleaseToMarkerNotification}
import uk.ac.warwick.tabula.coursework.MockRenderer
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.MarkerFeedback
import uk.ac.warwick.tabula.data.model.ModeratedMarkingWorkflow
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.data.model.{ModeratedMarkingWorkflow, Feedback, Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.coursework.MockRenderer
import uk.ac.warwick.userlookup.User

class ModeratorRejectedNotificationTest  extends TestBase with Mockito {

	val TEST_CONTENT = "test"
	val HERON_PLUG = "Not enough time has been given to explaining the awesome nature of Herons"

	def createNotification(agent: User, recipient: User, _object: MarkerFeedback, firstMarkerFeedback: MarkerFeedback) = {
		val n = new ModeratorRejectedNotification(agent, recipient, _object, firstMarkerFeedback) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	trait ModeratorRejectedNotificationFixture extends MarkingNotificationFixture {
		testAssignment.markingWorkflow = new ModeratedMarkingWorkflow
		val (f, mf1, mf2) = makeBothMarkerFeedback(student1)
		mf2.rejectionComments = HERON_PLUG
		mf2.mark = Some(41)
		mf2.grade = Some("3")
	}

	@Test
	def titleIncludesModuleAndAssignmentName(){ new ModeratorRejectedNotificationFixture {
		val n =  createNotification(marker2, marker1, mf1, mf2)
		n.title should be("Feedback rejected by the moderator for HERON101 - Test assignment")
	} }

	@Test
	def urlIsProfilePageForStudents():Unit = new ModeratorRejectedNotificationFixture{
		val n =  createNotification(marker2, marker1, mf1, mf2)
		n.url should be("/admin/module/heron101/assignments/1/marker/list")
	}


	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ModeratorRejectedNotificationFixture {
		val n =  createNotification(marker2, marker1, mf1, mf2)

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			Matchers.eq("/WEB-INF/freemarker/emails/moderator_rejected_notification.ftl"),
			any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ModeratorRejectedNotificationFixture {
		val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
		val n =  createNotification(marker2, marker1, mf1, mf2)

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			any[String],
			model.capture())

		model.getValue.get("markingUrl") should be(Some(n.url))
		model.getValue.get("assignment") should be(Some(testAssignment))
		model.getValue.get("studentId") should be(Some("student1"))
		model.getValue.get("moderatorName") should be(Some("Snorkeldink Wafflesmack"))
		model.getValue.get("rejectionComments") should be(Some(HERON_PLUG))
		model.getValue.get("adjustedMark") should be(Some(Some(41)))
		model.getValue.get("adjustedGrade") should be(Some(Some("3")))
	}
}
