package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.services.{UserLookupComponent, UserLookupService}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Description
import org.junit.Test
import uk.ac.warwick.tabula.groups.{SmallGroupEventBuilder, SmallGroupFixture, SmallGroupSetBuilder, SmallGroupBuilder}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.JavaImports.JArrayList
import org.mockito.Mockito._
import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.data.model.notifications.SmallGroupSetChangedNotification


class NotifiesAffectedGroupMembersTest extends TestBase {


  private trait Fixture extends SmallGroupFixture {
    val user1 = new User("user1")
    user1.setWarwickId("user1")
		user1.setUserId("user1")
    
    val user2 = new User("user2")
    user2.setWarwickId("user2")
		user2.setUserId("user2")

    val user3 = new User("user3")
    user3.setWarwickId("user3")
		user3.setUserId("user3")

    val user4 = new User("user4")
    user4.setWarwickId("user4")
		user4.setUserId("user4")
    
    val userDatabase = Seq(user1, user2, user3, user4)
		userLookup.getUsersByWarwickUniIds(any[Seq[String]]) answers { case ids: Seq[String @unchecked] =>
			ids.map(id => (id, userDatabase.find {_.getWarwickId == id}.getOrElse (new AnonymousUser()))).toMap
		}
		userLookup.getUserByUserId(any[String]) answers { case id: String @unchecked =>
			userDatabase.find {_.getUserId == id}.getOrElse (new AnonymousUser())
		}

    val eventA = new SmallGroupEventBuilder().withTutors(createUserGroup(Seq("tutor1","tutor2"),identifierIsUniNumber = false)).build
    val groupA = new SmallGroupBuilder()
      .withGroupName("groupA")
      .withStudents(createUserGroup(Seq("user1", "user2"),identifierIsUniNumber = true))
      .withEvents(Seq(eventA))
    val groupB = new SmallGroupBuilder().withStudents(createUserGroup(Seq("user3", "user4"),identifierIsUniNumber = true)).build
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
  def hasAffectedStudentsGroupsDetectsAdditions(){new Fixture {
    // add user4 to group A
    val modifiedGroupA = groupA.withStudents(createUserGroup(Seq("user1","user2","user4"))).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    command.hasAffectedStudentsGroups(user4) should be(true)
    command.hasAffectedStudentsGroups(user1) should be(false)
  }}

  @Test
  def hasAffectedStudentsGroupsDetectsRemovals(){new Fixture {
    // remove user2 from group A
    val modifiedGroupA = groupA.withStudents(createUserGroup(Seq("user1"))).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    command.hasAffectedStudentsGroups(user2) should be(true)
    command.hasAffectedStudentsGroups(user1) should be(false)
  }}

  @Test
  def hasAffectedStudentsGroupDetectsChangesToEvents(){new Fixture{
    val event = new SmallGroupEventBuilder().build
    val modifiedGroupA = groupA.withEvents(Seq(event)).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    // group A - affected
    command.hasAffectedStudentsGroups(user1) should be(true)
    command.hasAffectedStudentsGroups(user2) should be(true)

    // group B - unaffected
    command.hasAffectedStudentsGroups(user3) should be(false)

  }}

  @Test
  def emitsANotificationForEachAffectedStudent(){new Fixture{

    val event = new SmallGroupEventBuilder().build
    val modifiedGroupA = groupA.withEvents(Seq(event)).build
    command.set.groups = JArrayList(modifiedGroupA, groupB)

    val notifications = command.emit(command.set)
    notifications.size should be(2)

		notifications.foreach{
			case n: SmallGroupSetChangedNotification => n.userLookup = userLookup
		}

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

    cmd.emit(cmd.set) should be(Nil)
  }}

  @Test
  def createsFilteredGroupsetViewForTutors(){new Fixture {

    val addedEvent = new SmallGroupEventBuilder().build // tutor1 is not a tutor on this event
    val addedGroup =   new SmallGroupBuilder().copyOf(group1).withGroupName("addedGroup").withEvents(Seq(addedEvent)).build

    val event = new SmallGroupEventBuilder().build // tutor1 is not a tutor on this event
    group1.events.add(event)
    groupSet1.groups.add(addedGroup)

    groupSet1.groups.size should be(2)
    group1.events.size() should be(2)

    val filteredView = command.tutorsEvents(groupSet1,tutor1)
    filteredView.groups.size should be(1)
    filteredView.groups.asScala.head.events.size should be(1)

  }}

	@Test
	def detectsAllocationChangesForTutors() {new Fixture {

			val group = command.set.groups.asScala.find(_.name == "groupA").get
			val test: User = new User("test")
			test.setWarwickId("123")
			group.students.add(test)
			val tutor = group.events.asScala.head.tutors.users.head
			command.hasAffectedTutorsEvents(tutor) should be(true)
	}}


  class StubCommand(val set: SmallGroupSet, val apparentUser: User, var userLookup: UserLookupService)
    extends SmallGroupSetCommand with UserLookupComponent with NotifiesAffectedGroupMembers {
  }

}
