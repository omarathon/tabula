package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.notifications.groups.OpenSmallGroupSetsNotification
import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.data.model.Notification

class OpenGroupSetNotificationTest extends TestBase with Mockito {

	def createNotification(groups: Seq[SmallGroupSet], actor: User, recipient: User) = {
		val n = Notification.init(new OpenSmallGroupSetsNotification, actor, groups)
		n.recipientUserId = recipient.getUserId
		n
	}

	@Test
	def titleIncludesGroupFormats() { new SmallGroupFixture {
		val n =  createNotification(Seq(groupSet1, groupSet2, groupSet3), actor, recipient)
		n.userLookup = userLookup
		n.title should be("Lab, Seminar and Tutorial groups are now open for sign up.")
	}}

	@Test
	def titleMakesSenseWithASingleGroup() { new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1),actor, recipient)
		n.userLookup = userLookup
		n.title should be ("Lab groups are now open for sign up.")
	}}

	@Test
	def urlIsSmallGroupTeachingHomePage(): Unit = new SmallGroupFixture {
		val n =  createNotification(Seq(groupSet1), actor, recipient)
		n.userLookup = userLookup
		n.url should be("/groups")
	}

	@Test
	def recipientsContainsSingleUser(): Unit  = new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1), actor, recipient)
		n.userLookup = userLookup
		n.recipients should be (Seq(recipient))
	}

	@Test
	def shouldCallTextRendererWithCorrectTemplate(): Unit = new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1), actor, recipient)
		n.userLookup = userLookup
		n.content.template should be ("/WEB-INF/freemarker/notifications/groups/open_small_group_student_notification.ftl")
	}

	@Test
	def shouldCallTextRendererWithCorrectModel(): Unit = new SmallGroupFixture {
		val n = createNotification(Seq(groupSet1), actor, recipient)
		val content = n.content
		content.model("profileUrl") should be("/groups")
		content.model("groupsets") should be(Seq(groupSet1))
	}

}
