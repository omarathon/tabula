package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.ExtensionFixture
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.notifications.{ExtensionRequestRespondedRejectNotification, ExtensionRequestRespondedApproveNotification}

class ExtensionRequestRespondedNotificationTest extends TestBase with Mockito with ExtensionNotificationTesting {

	def createNotification(extension: Extension, student: User, actor: User) = {
		val baseNotification = if (extension.approved) {
			new ExtensionRequestRespondedApproveNotification
		} else {
			new ExtensionRequestRespondedRejectNotification
		}
		val n = Notification.init(baseNotification, actor, Seq(extension), extension.assignment)
		wireUserlookup(n, student)
		n
	}


	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student, admin)
		 n.url should be("/coursework/admin/module/xxx/assignments/123/extensions/detail")
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
		 n.content.template should be ("/WEB-INF/freemarker/emails/responded_extension_request.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n = createNotification(extension, student, admin)
		n.content.model.get("studentName").get should be("[Unknown user]")
		n.content.model.get("agentName").get should be("[Unknown user]")
		n.content.model.get("newExpiryDate").get should be("23 August 2013 at 12:00:00")
		n.content.model.get("assignment").get should be(assignment)
		n.content.model.get("verbed").get should be("rejected")
		n.content.model.get("path").get should be("/coursework/admin/module/xxx/assignments/123/extensions/detail")
	 }
 }
