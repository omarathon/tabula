package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.data.model.notifications.groups.{SmallGroupSetChangedTutorNotification, SmallGroupSetChangedStudentNotification, SmallGroupSetChangedNotification}
import uk.ac.warwick.tabula.{TestBase, Mockito}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.data.model.notifications.SmallGroupSetChangedTutorNotification
import uk.ac.warwick.tabula.data.model.Notification

class SmallGroupSetChangedNotificationTest extends TestBase with Mockito {

  def createStudentNotification(groupSet:SmallGroupSet, actor:User, recipient:User) = {
		val n = Notification.init(new SmallGroupSetChangedStudentNotification, actor, groupSet.groups.asScala, groupSet)
		n.recipientUserId = recipient.getUserId
		n
  }

	def createTutorNotification(groupSet:SmallGroupSet, actor:User, recipient:User) = {
		val n = Notification.init(new SmallGroupSetChangedTutorNotification, actor, groupSet.groups.asScala, groupSet)
		n.recipientUserId = recipient.getUserId
		n
	}

  @Test
  def urlIsProfilePageForStudent():Unit = new SmallGroupFixture{
    val n =  createStudentNotification(groupSet1, actor, recipient)
    n.url should be("/profiles/view/me")
  }


  @Test
  def urlIsGroupsPageForTutor(): Unit = new SmallGroupFixture {
    val n = createTutorNotification(groupSet1, actor, recipient)
    n.url should be(Routes.tutor.mygroups)
  }

  @Test
  def titleIsHardcoded(){new SmallGroupFixture {
    val n =  createStudentNotification(groupSet1, actor, recipient)
    n.title should be("Changes to small group allocation")
  }}

  @Test
  def shouldCallTextRendererWithCorrectTemplateAndModel():Unit = new SmallGroupFixture {
    val n = createStudentNotification(groupSet1, actor, recipient)
    n.content.template should be (SmallGroupSetChangedNotification.templateLocation)
		n.content.model.get("profileUrl") should be(Some("/profiles/view/me"))
		n.content.model.get("groupSet") should be(Some(groupSet1))
  }

}
