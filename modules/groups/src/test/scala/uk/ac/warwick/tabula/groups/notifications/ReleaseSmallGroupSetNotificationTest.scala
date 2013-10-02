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

class ReleaseSmallGroupSetNotificationTest extends TestBase with Mockito{

  val TEST_CONTENT = "test"
  def createNotification(group:SmallGroup, actor:User,recipient:User, isStudent:Boolean = true) = {
    val n = new ReleaseSmallGroupSetNotification(group, actor, recipient, isStudent) with MockRenderer
    when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
    n
  }

  @Test
  def titleIncludesGroupFormat(){new SmallGroupFixture {
    val n =  createNotification(group1, actor, recipient)
    n.title should be("Lab allocation")
  }}

  @Test
  def urlIsProfilePageForStudents():Unit = new SmallGroupFixture{

    val n =  createNotification(group1, actor, recipient, isStudent = true)
    n.url should be("/profiles/view/recipient")

  }

  @Test
  def urlIsMyGroupsPageForTutors():Unit = new SmallGroupFixture{

    val n =  createNotification(group1, actor, recipient, isStudent = false)
    n.url should be("/groups" + Routes.tutor.mygroups(recipient))

  }

  @Test
  def recipientsContainsSingleUser():Unit  = new SmallGroupFixture{
    val n = createNotification(group1, actor, recipient)
    n.recipients should be (Seq(recipient))
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate():Unit = new SmallGroupFixture {
    val n = createNotification(group1, actor, recipient)

    n.content should be (TEST_CONTENT)

    verify(n.mockRenderer, times(1)).renderTemplate(
      Matchers.eq("/WEB-INF/freemarker/notifications/release_small_group_student_notification.ftl"),
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
    model.getValue.get("profileUrl") should be(Some("/profiles/view/recipient"))
    model.getValue.get("group") should be(Some(group1))
  }

  trait MockRenderer extends TextRenderer{
    val mockRenderer = mock[TextRenderer]
    def renderTemplate(id:String,model:Any ):String = {
      mockRenderer.renderTemplate(id, model)
    }
  }
}

