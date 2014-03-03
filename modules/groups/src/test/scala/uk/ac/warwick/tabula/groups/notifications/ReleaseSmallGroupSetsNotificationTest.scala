package uk.ac.warwick.tabula.groups.notifications


import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.data.model.notifications.ReleaseSmallGroupSetsNotification
import uk.ac.warwick.tabula.data.model.Notification

class ReleaseSmallGroupSetsNotificationTest extends TestBase with Mockito{

  val TEST_CONTENT = "test"
  def createNotification(group:SmallGroup, actor:User,recipient:User, isStudent:Boolean = true) =
		createMultiGroupNotification(Seq(group), actor, recipient, isStudent)

  def createMultiGroupNotification(groups:Seq[SmallGroup], actor:User,recipient:User, isStudent:Boolean = true): ReleaseSmallGroupSetsNotification = {
    val n = Notification.init(new ReleaseSmallGroupSetsNotification, actor, groups)
    n.recipientUserId = recipient.getUserId
		n.isStudent = isStudent
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
  def cantCreateANotificationWithNoGroups(){ new SmallGroupFixture{
    val n = createMultiGroupNotification(Nil, actor, recipient)
		n.preSave(true)
  }}

  @Test
  def urlIsProfilePageForStudents():Unit = new SmallGroupFixture{

    val n =  createNotification(group1, actor, recipient, isStudent = true)
    n.url should be("/profiles/view/me")

  }

  @Test
  def urlIsMyGroupsPageForTutors():Unit = new SmallGroupFixture{
    val n =  createNotification(group1, actor, recipient, isStudent = false)
    n.url should be(Routes.tutor.mygroups)
  }

  @Test
  def shouldCallTextRendererWithCorrectTemplate():Unit = new SmallGroupFixture {
    val n = createNotification(group1, actor, recipient)
		n.userLookup = userLookup
    n.content.template should be ("/WEB-INF/freemarker/notifications/release_small_group_notification.ftl")
  }

  @Test
  def shouldCallTextRendererWithCorrectModel():Unit = new SmallGroupFixture {
    val n = createNotification(group1, actor, recipient)
		n.userLookup = userLookup
    n.content.model.get("user") should be(Some(recipient))
    n.content.model.get("profileUrl") should be(Some("/profiles/view/me"))
    n.content.model.get("groups") should be(Some(List(group1)))
  }
}

