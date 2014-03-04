package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.ExtensionFixture
import uk.ac.warwick.tabula.data.model.notifications.ExtensionGrantedNotification
import uk.ac.warwick.tabula.data.model.Notification

class ExtensionGrantedNotificationTest extends TestBase with Mockito with ExtensionNotificationTesting {

	def createNotification(extension: Extension, student: User, actor: User) = {
		val n = Notification.init(new ExtensionGrantedNotification, actor, Seq(extension), extension.assignment)
		wireUserlookup(n, student)
		n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		val n = createNotification(extension, student, admin)
		n.url should be("/coursework/module/xxx/123/")
	}

	@Test
	def recipientsContainsSingleUser():Unit = new ExtensionFixture{
		val n = createNotification(extension, student, admin)
		n.recipients should be (Seq(student))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate():Unit = new ExtensionFixture {
		val n = createNotification(extension, student, admin)
		n.content.template should be ("/WEB-INF/freemarker/emails/new_manual_extension.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n = createNotification(extension, student, admin)
		n.content.model.get("extension").get should be(extension)
		n.content.model.get("newExpiryDate").get should be("23 August 2013 at 12:00:00")
		n.content.model.get("assignment").get should be(assignment)
		n.content.model.get("module").get should be(module)
		n.content.model.get("user").get should be(student)
		n.content.model.get("path").get should be("/coursework/module/xxx/123/")
	}
}
