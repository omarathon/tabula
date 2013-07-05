package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.web.views.TextRenderer
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import org.mockito.Mockito._
import uk.ac.warwick.tabula.profiles.TutorFixture
import org.mockito.{Matchers, ArgumentCaptor}
import uk.ac.warwick.tabula.profiles.notifications.TutorChangeNotification

class TutorChangeNotificationTest extends TestBase with Mockito {

	val TEST_CONTENT = "test"

	def createNewTutorNotification(relationship:StudentRelationship, actor:User, recipient:User, oldTutor: Option[Member]) = {
		val n = new TutorChangeNotification(relationship, actor, recipient, oldTutor, TutorChangeNotification.NewTutorTemplate) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	def createOldTutorNotification(relationship:StudentRelationship, actor:User, recipient:User, oldTutor: Option[Member]) = {
		val n = new TutorChangeNotification(relationship, actor, recipient, oldTutor, TutorChangeNotification.OldTutorTemplate) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	def createTuteeNotification(relationship:StudentRelationship, actor:User, recipient:User, oldTutor: Option[Member]) = {
		val n = new TutorChangeNotification(relationship, actor, recipient, oldTutor, TutorChangeNotification.TuteeTemplate) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	@Test
	def urlIsProfilePage():Unit = new TutorFixture{
		val n = createNewTutorNotification(relationship, actor, recipient, Some(oldTutor))
		n.url should be("/view/student")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new TutorFixture{
		val n = createOldTutorNotification(relationship, actor, recipient, Some(oldTutor))
		n.recipients should be (Seq(recipient))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new TutorFixture {
		val n = createTuteeNotification(relationship, actor, recipient, Some(oldTutor))
		n.content should be (TEST_CONTENT)
		verify(n.mockRenderer, times(1)).renderTemplate(
			Matchers.eq("/WEB-INF/freemarker/notifications/tutor_change_tutee_notification.ftl"),
			any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new TutorFixture {
		val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])

		val n = createNewTutorNotification(relationship, actor, recipient, Some(oldTutor))

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			any[String],
			model.capture())

		model.getValue.get("tutee").get should be(student)
		model.getValue.get("oldTutor").get should be(Some(oldTutor))
		model.getValue.get("newTutor").get should be(Some(newTutor))
		model.getValue.get("path").get should be("/view/student")
	}

	trait MockRenderer extends TextRenderer {
		val mockRenderer = mock[TextRenderer]
		def renderTemplate(id:String,model:Any ):String = {
			mockRenderer.renderTemplate(id, model)
		}
	}
}
