package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Description
import org.junit.Test
import uk.ac.warwick.tabula.groups.{SmallGroupEventBuilder, SmallGroupFixture, SmallGroupSetBuilder, SmallGroupBuilder}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.JavaImports.JArrayList
import org.mockito.Mockito._


class NotifiesAffectedGroupMembersTest extends TestBase {


  private trait Fixture extends SmallGroupFixture {
    val user1 = new User("user1")
    val user2 = new User("user2")
    val user3 = new User("user3")
    when(userLookup.getUserByWarwickUniId("user1")).thenReturn(user1)
    when(userLookup.getUserByWarwickUniId("user2")).thenReturn(user2)
    when(userLookup.getUserByWarwickUniId("user3")).thenReturn(user3)

    val groupA = new SmallGroupBuilder().withStudentIds(Seq("user1", "user2"))
    val groupB = new SmallGroupBuilder().withStudentIds(Seq("user3", "user4")).build
    val groupSet = new SmallGroupSetBuilder().withReleasedToStudents(true).withGroups(Seq(groupA.build, groupB))

    val command: StubCommand = new StubCommand(groupSet.build, actor, userLookup)

  }
  @Test
  def createsSnapshotOfSmallGroupWhenConstructed() {
    new SmallGroupFixture {
      val command: StubCommand = new StubCommand(groupSet1, actor, userLookup)

      command.setBeforeUpdates should equal(groupSet1)
      command.setBeforeUpdates.eq(groupSet1) should be(false)

    }
  }

  @Test
  def createNotificationCreatesNotificationForCorrectGroup {
    new Fixture {

      // de-allocate user2 from group A
      val modifiedGroupA = groupA.withStudentIds(Seq("user1")).build
      command.set.groups = JArrayList(modifiedGroupA, groupB)

      val user1Notification  = command.createNotification("user1").get
      user1Notification._object should be(modifiedGroupA)
      user1Notification.recipients should be(Seq(user1))

      val user3Notification = command.createNotification("user3").get
      user3Notification._object should be(groupB)
      user3Notification.recipients should be(Seq(user3))

      val inNoGroupNotification = command.createNotification("not-in-any-group")
      inNoGroupNotification should be(None)
    }
  }

  @Test
  def hasAffectedStudentsGroupsDetectsAdditions(){new Fixture {
    // add user4 to group A
    val modifiedGroupA = groupA.withStudentIds(Seq("user1","user2","user4")).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    command.hasAffectedStudentsGroups("user4") should be(true)
    command.hasAffectedStudentsGroups("user1") should be(false)
  }}

  @Test
  def hasAffectedStudentsGroupsDetectsRemovals(){new Fixture {
    // remove user2 from group A
    val modifiedGroupA = groupA.withStudentIds(Seq("user1")).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    command.hasAffectedStudentsGroups("user2") should be(true)
    command.hasAffectedStudentsGroups("user1") should be(false)
  }}

  @Test
  def hasAffectedStudentsGroupDetectsChangesToEvents(){new Fixture{
    val event = new SmallGroupEventBuilder().build
    val modifiedGroupA = groupA.withEvents(Seq(event)).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    // group A - affected
    command.hasAffectedStudentsGroups("user1") should be(true)
    command.hasAffectedStudentsGroups("user2") should be(true)

    // group B - unaffected
    command.hasAffectedStudentsGroups("user3") should be(false)

  }}

  @Test
  def emitsANotificationForEachAffectedStudent(){new Fixture{

    val event = new SmallGroupEventBuilder().build
    val modifiedGroupA = groupA.withEvents(Seq(event)).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    val notifications = command.emit
    notifications.size should be(2)

    notifications.exists(_.recipients == Seq(user1)) should be(true)
    notifications.exists(_.recipients == Seq(user2)) should be(true)
    notifications.exists(_.recipients == Seq(user3)) should be(false)
  }}


  @Test
  def emitsNoNotificationsToStudentsIfGroupsetIsNotReleased{new Fixture {
    val unreleasedGroupset =groupSet.withReleasedToStudents(false).build
    val cmd = new StubCommand(unreleasedGroupset, actor, userLookup)
    val event = new SmallGroupEventBuilder().build
    val modifiedGroupA = groupA.withEvents(Seq(event)).build
    cmd.set.groups = JArrayList(modifiedGroupA, groupB)

    cmd.emit should be(Nil)
  }

  }


  class StubCommand(val set: SmallGroupSet, val apparentUser: User, var userLookup: UserLookupService)
    extends SmallGroupSetCommand with NotifiesAffectedGroupMembers {
  }

}
