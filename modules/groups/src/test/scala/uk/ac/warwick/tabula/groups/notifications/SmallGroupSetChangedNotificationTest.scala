package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.groups.web.Routes

class SmallGroupSetChangedNotificationTest extends TestBase with Mockito {

  val TEST_CONTENT = "test"
  def createNotification(groupSet:SmallGroupSet, actor:User,recipient:User, role:UserRoleOnGroup = UserRoleOnGroup.Student):
  SmallGroupSetChangedNotification with MockRenderer = {
    val n = new SmallGroupSetChangedNotification(groupSet, actor, recipient,role) with MockRenderer
    when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
    n
  }

  @Test
  def urlIsProfilePageForStudent():Unit = new SmallGroupFixture{
    val n =  createNotification(groupSet1, actor, recipient)
    n.url should be("/profiles/view/recipient")
  }


  @Test
  def urlIsGroupsPageForTutor(): Unit = new SmallGroupFixture {
    val n = createNotification(groupSet1, actor, recipient, UserRoleOnGroup.Tutor)
    n.url should be("/groups" + Routes.tutor.mygroups(recipient))
  }

  @Test
  def titleIsHardcoded(){new SmallGroupFixture {
    val n =  createNotification(groupSet1, actor, recipient)
    n.title should be("Changes to small group allocation")
  }}

  @Test
  def shouldCallTextRendererWithCorrectTemplate():Unit = new SmallGroupFixture {
    val n = createNotification(groupSet1, actor, recipient)

    n.content should be (TEST_CONTENT)

    verify(n.mockRenderer, times(1)).renderTemplate(
      Matchers.eq(SmallGroupSetChangedNotification.templateLocation),
      any[Map[String,Any]])
  }

  @Test
  def shouldCallTextRendererWithCorrectModel():Unit = new SmallGroupFixture {
    val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
    val n = createNotification(groupSet1, actor, recipient)

    n.content should be (TEST_CONTENT)

    verify(n.mockRenderer, times(1)).renderTemplate(
      any[String],
      model.capture())

    model.getValue.get("profileUrl") should be(Some("/profiles/view/recipient"))
    model.getValue.get("groupSet") should be(Some(groupSet1))
  }
}
