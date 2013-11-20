package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications.{ExtensionRequestCreatedNotification, ExtensionRequestRespondedNotification}
import uk.ac.warwick.tabula.coursework.{ExtensionFixture, MockRenderer}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}

class ExtensionRequestCreatedNotificationTest extends TestBase with Mockito {

	val TEST_CONTENT = "test"

	def createNotification(extension: Extension, student: User) = {
		 val extraInfo = Map("studentMember" -> student)
		 val n = new ExtensionRequestCreatedNotification(extension, student, extraInfo) with MockRenderer
		 when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		 n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student)
		 n.url should be("/admin/module/xxx/assignments/123/extensions?highlight=student")
	}

	@Test
	def titleShouldContainMessage():Unit = new ExtensionFixture {
		val n = createNotification(extension, student)
		 n.title.contains("New extension request made") should be(true)
	}


	@Test
	def recipientsContainsAllAdmins():Unit = new ExtensionFixture{
		val n = createNotification(extension, student)
		 n.recipients should be (Seq(admin, admin2, admin3))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n = createNotification(extension, student)
		 n.content should be (TEST_CONTENT)
		 verify(n.mockRenderer, times(1)).renderTemplate(
			 Matchers.eq("/WEB-INF/freemarker/emails/new_extension_request.ftl"),
			 any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n = createNotification(extension, student)
		 n.content should be (TEST_CONTENT)
		 val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
		 verify(n.mockRenderer, times(1)).renderTemplate(
			 any[String],
			 model.capture()
		 )
		 model.getValue.get("requestedExpiryDate").get should be("23 August 2013 at 12:00:00")
		 model.getValue.get("reasonForRequest").get should be("My hands have turned to flippers. Like the ones that dolphins have. It makes writing and typing super hard. Pity me.")
		model.getValue.get("path").get should be("/admin/module/xxx/assignments/123/extensions?highlight=student")
		model.getValue.get("assignment").get should be(assignment)
		model.getValue.get("student").get should be(student)
	 }
 }
