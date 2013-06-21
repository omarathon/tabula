package uk.ac.warwick.tabula.groups.notifications

import uk.ac.warwick.tabula.{TestBase, Mockito}
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.userlookup.User
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}

class SmallGroupSetChangedNotificationTest extends TestBase with Mockito {

  val TEST_CONTENT = "test"
  def createNotification(group:SmallGroup, actor:User,recipient:User):SmallGroupSetChangedNotification with MockRenderer = {
    val n = new SmallGroupSetChangedNotification(group, actor, recipient) with MockRenderer
    when(n.mockRenderer.renderTemplate(any[String],any[Any])).thenReturn(TEST_CONTENT)
    n
  }

  @Test
  def urlIsProfilePage():Unit = new SmallGroupFixture{
    val n =  createNotification(group1, actor, recipient)
    n.url should be("/view/recipient")
  }

  @Test
  def titleIsHardcoded(){new SmallGroupFixture {
    val n =  createNotification(group1, actor, recipient)
    n.title should be("Changes to small group allocation")
  }}

  @Test
  def receipientIsStudent(){new SmallGroupFixture {
    val n =  createNotification(group1, actor, recipient)
    n.recipients should be(Seq(recipient))
  }}

  @Test
  def shouldCallTextRendererWithCorrectTemplate():Unit = new SmallGroupFixture {
    val n = createNotification(group1, actor, recipient)

    n.content should be (TEST_CONTENT)

    verify(n.mockRenderer, times(1)).renderTemplate(
      Matchers.eq(SmallGroupSetChangedNotification.templateLocation),
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

    model.getValue.get("student") should be(Some(recipient))
    model.getValue.get("profileUrl") should be(Some("/view/recipient"))
    model.getValue.get("group") should be(Some(group1))
  }
}
