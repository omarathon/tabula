package uk.ac.warwick.tabula.groups.notifications

import org.junit.Test
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import org.mockito.Mockito._

import uk.ac.warwick.tabula.web.views.TextRenderer
import org.mockito.{ArgumentCaptor, Matchers}
import uk.ac.warwick.tabula.groups.web.Routes

class ReleaseSmallGroupSetsNotificationTest extends TestBase with Mockito{

  val TEST_CONTENT = "test"
  def createNotification(group:SmallGroup, actor:User,recipient:User, isStudent:Boolean = true):ReleaseSmallGroupSetsNotification with MockRenderer = {
    createMultiGroupNotification(Seq(group), actor, recipient, isStudent)
  }

  def createMultiGroupNotification(groups:Seq[SmallGroup], actor:User,recipient:User, isStudent:Boolean = true) = {
    val n = new ReleaseSmallGroupSetsNotification(groups, actor, recipient, isStudent) with MockRenderer
    when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
    n
  }

  @Test
  def titleIncludesGroupFormat(){new SmallGroupFixture {
    val n =  createNotification(group1, actor, recipient)
    n.title should be("Lab allocation")
  }}

  @Test
  def titleJoinsMultipleGroupSetsNicely(){ new SmallGroupFixture{
    val n = createMultiGroupNotification(Seq(group1,group2, group3),actor, recipient)
    n.title should be ("Lab, Seminar and Tutorial allocation")
  }}

  @Test
  def titleRemovesDuplicateFormats(){ new SmallGroupFixture{
    val n = createMultiGroupNotification(Seq(group1,group2, group3, group4, group5),actor, recipient)
    n.title should be ("Lab, Seminar and Tutorial allocation")
  }}

  @Test(expected = classOf[IllegalArgumentException])
  def cantCreateANotificationWithNoGroups(){new SmallGroupFixture{
    val n = createMultiGroupNotification(Nil,actor, recipient)
  }}

  @Test
  def urlIsProfilePageForStudents():Unit = new SmallGroupFixture{

    val n =  createNotification(group1, actor, recipient, isStudent = true)
    n.url should be("/view/recipient")

  }

  @Test
  def urlIsMyGroupsPageForTutors():Unit = new SmallGroupFixture{

    val n =  createNotification(group1, actor, recipient, isStudent = false)
    n.url should be(Routes.tutor.mygroups(recipient))

  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate():Unit = new SmallGroupFixture {
    val n = createNotification(group1, actor, recipient)

    n.content should be (TEST_CONTENT)

    verify(n.mockRenderer, times(1)).renderTemplate(
      Matchers.eq("/WEB-INF/freemarker/notifications/release_small_group_notification.ftl"),
      any[Map[String,Any]])
  }

  @Test
  def shouldCallTextRendererWithCorrectModel():Unit = new SmallGroupFixture {
    val model = ArgumentCaptor.forClass(classOf[Map[String,Any]])
    val n = createNotification(group1, actor, recipient)

    n.content should be (TEST_CONTENT)

    verify(n.mockRenderer, times(1)).renderTemplate(
      any[String],
      model.capture())

    model.getValue.get("user") should be(Some(recipient))
    model.getValue.get("profileUrl") should be(Some("/view/recipient"))
    model.getValue.get("groups") should be(Some(List(group1)))
  }
}

