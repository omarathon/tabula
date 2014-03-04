package uk.ac.warwick.tabula.coursework.notifications

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.ExtensionFixture
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.notifications.ExtensionRequestCreatedNotification
import uk.ac.warwick.tabula.services.{RelationshipService, ProfileService, UserLookupService}

class ExtensionRequestCreatedNotificationTest extends TestBase with ExtensionNotificationTesting with Mockito {

	def createNotification(extension: Extension, student: User) = {
		val n = Notification.init(new ExtensionRequestCreatedNotification, student, Seq(extension), extension.assignment)
		n.userLookup = mockUserLookup
		n.profileService = mockProfileService
		n.relationshipService = mockRelationshipService

		wireUserlookup(n, student)
		n.profileService.getMemberByUniversityId(student.getWarwickId) returns (None)

		n
	}

	@Test
	def urlIsProfilePage():Unit = new ExtensionFixture {
		 val n = createNotification(extension, student)
		 n.url should be("/coursework/admin/module/xxx/assignments/123/extensions?highlight=student")
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
		n.content.template should be ("/WEB-INF/freemarker/emails/new_extension_request.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel():Unit = new ExtensionFixture {
		val n = createNotification(extension, student)
		n.content.model.get("requestedExpiryDate").get should be("23 August 2013 at 12:00:00")
		n.content.model.get("reasonForRequest").get should be("My hands have turned to flippers. Like the ones that dolphins have. It makes writing and typing super hard. Pity me.")
		n.content.model.get("path").get should be("/coursework/admin/module/xxx/assignments/123/extensions?highlight=student")
		n.content.model.get("assignment").get should be(assignment)
		n.content.model.get("student").get should be(student)
	 }
 }
