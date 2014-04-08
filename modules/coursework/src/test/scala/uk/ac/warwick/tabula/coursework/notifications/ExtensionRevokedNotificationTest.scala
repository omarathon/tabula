package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.ExtensionFixture
import uk.ac.warwick.tabula.data.model.{Notification, Assignment}
import uk.ac.warwick.tabula.data.model.notifications.ExtensionRevokedNotification

class ExtensionRevokedNotificationTest extends TestBase with Mockito with ExtensionNotificationTesting {


	def createNotification(assignment: Assignment, student: User, actor: User) = {
		val n = Notification.init(new ExtensionRevokedNotification, actor, Seq(assignment))
		wireUserlookup(n, student)
		n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		val n = createNotification(assignment, student, admin)
		n.url should be("/coursework/module/xxx/123/")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new ExtensionFixture{
		val n = createNotification(assignment, student, admin)
		n.recipients should be (Seq(student))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n = createNotification(assignment, student, admin)
		n.content.template should be ("/WEB-INF/freemarker/emails/revoke_manual_extension.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n = createNotification(assignment, student, admin)
		n.content.model("originalAssignmentDate") should be("1 August 2013 at 12:00:00")
		n.content.model("assignment") should be(assignment)
		n.content.model("module") should be(module)
		n.content.model("user") should be(student)
		n.url should be("/coursework/module/xxx/123/")
	}
}
