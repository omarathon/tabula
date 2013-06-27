package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications.{ExtensionRequestRespondedNotification, ExtensionRequestRejectedNotification}
import uk.ac.warwick.tabula.coursework.{ExtensionFixture, MockRenderer}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}

class ExtensionRequestRespondedNotificationTest extends TestBase with Mockito {

	val TEST_CONTENT = "test"

	def createNotification(extension: Extension, student: User, actor: User) = {
		 val n = new ExtensionRequestRespondedNotification(extension, student, actor) with MockRenderer
		 when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		 n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student, admin)
		 n.url should be("/admin/module/xxx/assignments/123/extensions?highlight=student")
	}

	@Test
	def titleShouldContainMessage():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student, admin)
		 n.title.contains("XXX: Extension request by [Unknown user] was rejected") should be(true)
	}


	@Test
	def recipientsContainsOtherAdmins():Unit = new ExtensionFixture{
		 val n = createNotification(extension, student, admin)
		 n.recipients should be (Seq(admin2, admin3))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student, admin)
		 n.content should be (TEST_CONTENT)
		 verify(n.mockRenderer, times(1)).renderTemplate(
			 Matchers.eq("/WEB-INF/freemarker/emails/responded_extension_request.ftl"),
			 any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student, admin)
		 n.content should be (TEST_CONTENT)
		 val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
		 verify(n.mockRenderer, times(1)).renderTemplate(
			 any[String],
			 model.capture()
		 )
		 model.getValue.get("studentName").get should be("[Unknown user]")
		 model.getValue.get("agentName").get should be("[Unknown user]")
		 model.getValue.get("newExpiryDate").get should be("23 August 2013 at 12:00:00")
		 model.getValue.get("assignment").get should be(assignment)
		 model.getValue.get("verbed").get should be("rejected")
		 model.getValue.get("path").get should be("/admin/module/xxx/assignments/123/extensions?highlight=student")
	 }
 }
