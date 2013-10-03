package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import scala.Some
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.groups.web.Routes
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.web.views.TextRenderer

class OpenGroupSetNotificationTest extends TestBase with Mockito {

	val TEST_CONTENT = "test"
	def createNotification(groups: Seq[SmallGroupSet], actor: User,recipient: User) = {
		val n = new OpenSmallGroupSetsNotification(actor, recipient, groups) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	@Test
	def titleIncludesGroupFormats() { new SmallGroupFixture {
		val n =  createNotification(Seq(groupSet1, groupSet2, groupSet3), actor, recipient)
		n.title should be("Lab, Seminar and Tutorial groups are now open for sign up.")
	}}

	@Test
	def titleMakesSenseWithASingleGroup() { new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1),actor, recipient)
		n.title should be ("Lab groups are now open for sign up.")
	}}

	@Test
	def urlIsSmallGroupTeachingHomePage(): Unit = new SmallGroupFixture {
		val n =  createNotification(Seq(groupSet1), actor, recipient)
		n.url should be("/groups")
	}

	@Test
	def recipientsContainsSingleUser(): Unit  = new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1), actor, recipient)
		n.recipients should be (Seq(recipient))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate(): Unit = new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1), actor, recipient)

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			Matchers.eq("/WEB-INF/freemarker/notifications/open_small_group_student_notification.ftl"),
			any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel(): Unit = new SmallGroupFixture {
		val model = ArgumentCaptor.forClass(classOf[Map[String, Any]])
		val n = createNotification(Seq(groupSet1), actor, recipient)

		n.content should be (TEST_CONTENT)

		verify(n.mockRenderer, times(1)).renderTemplate(
			any[String],
			model.capture())

		model.getValue.get("user") should be(Some(recipient))
		model.getValue.get("groupsets") should be(Some(Seq(groupSet1)))
	}

	trait MockRenderer extends TextRenderer{
		val mockRenderer = mock[TextRenderer]
		def renderTemplate(id: String, model: Any ): String = {
			mockRenderer.renderTemplate(id, model)
		}
	}
}
