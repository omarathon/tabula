package uk.ac.warwick.tabula.coursework.commands.markerfeedback


import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.notifications.ModeratorRejectedNotification

class ModerationRejectedNotificationTest  extends TestBase with Mockito {

	val HERON_PLUG = "Not enough time has been given to explaining the awesome nature of Herons"

	def createNotification(agent: User, recipient: User, markerFeedback: MarkerFeedback) = {
		val n =  Notification.init(new ModeratorRejectedNotification, agent, Seq(markerFeedback))
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
		val n =  createNotification(marker2, marker1, mf1)
		n.title should be("Feedback rejected by the moderator for HERON101 - Test assignment")
	} }

	@Test
	def urlIsProfilePageForStudents():Unit = new ModeratorRejectedNotificationFixture{
		val n =  createNotification(marker2, marker1, mf1)
		n.url should be("/coursework/admin/module/heron101/assignments/1/marker/list")
	}


	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ModeratorRejectedNotificationFixture {
		val n =  createNotification(marker2, marker1, mf1)
		n.content.template should be("/WEB-INF/freemarker/emails/moderator_rejected_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ModeratorRejectedNotificationFixture {

		val n =  createNotification(marker2, marker1, mf1)
		n.content.model.get("markingUrl") should be(Some(n.url))
		n.content.model.get("assignment") should be(Some(testAssignment))
		n.content.model.get("studentId") should be(Some("student1"))
		n.content.model.get("moderatorName") should be(Some("Snorkeldink Wafflesmack"))
		n.content.model.get("rejectionComments") should be(Some(HERON_PLUG))
		n.content.model.get("adjustedMark") should be(Some(Some(41)))
		n.content.model.get("adjustedGrade") should be(Some(Some("3")))
	}
}
