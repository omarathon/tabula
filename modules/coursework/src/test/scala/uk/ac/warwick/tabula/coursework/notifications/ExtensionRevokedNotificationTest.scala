package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.commands.assignments.extensions.notifications.{ExtensionRevokedNotification, ExtensionGrantedNotification}
import uk.ac.warwick.tabula.coursework.{ExtensionFixture, MockRenderer}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.data.model.Assignment

class ExtensionRevokedNotificationTest extends TestBase with Mockito {

	val TEST_CONTENT = "test"

	def createNotification(assignment: Assignment, student: User, actor: User) = {
		val n = new ExtensionRevokedNotification(assignment, student, actor) with MockRenderer
		when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
		n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		val n = createNotification(assignment, student, admin)
		n.url should be("/module/xxx/123/")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new ExtensionFixture{
		val n = createNotification(assignment, student, admin)
		n.recipients should be (Seq(student))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n = createNotification(assignment, student, admin)
		n.content should be (TEST_CONTENT)
		verify(n.mockRenderer, times(1)).renderTemplate(
			Matchers.eq("/WEB-INF/freemarker/emails/revoke_manual_extension.ftl"),
			any[Map[String,Any]])
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n = createNotification(assignment, student, admin)
		n.content should be (TEST_CONTENT)
		val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
		verify(n.mockRenderer, times(1)).renderTemplate(
			any[String],
			model.capture()
		)
		model.getValue.get("originalAssignmentDate").get should be("1 August 2013 at 12:00:00")
		model.getValue.get("assignment").get should be(assignment)
		model.getValue.get("module").get should be(module)
		model.getValue.get("user").get should be(student)
		model.getValue.get("path").get should be("/module/xxx/123/")
	}
}
