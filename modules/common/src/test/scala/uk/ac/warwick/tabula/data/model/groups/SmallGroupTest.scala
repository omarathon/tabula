package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.{Mockito, TestBase}
import junit.framework.Test
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.data.model.UserGroup
import org.mockito.Mockito._
import org.mockito.Matchers._
import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.SmallGroupService

class SmallGroupTest extends TestBase with Mockito {

  val event = newEventWithMockedServices

  val equivalentEvent = newEventWithMockedServices

  val notEquivalentEvent = newEventWithMockedServices
  notEquivalentEvent.day = DayOfWeek.Monday

	def newSmallGroupWithMockedServices = {
		val group = new SmallGroup()
		group.permissionsService = mock[PermissionsService]
		group.smallGroupService = None
		group
	}

	def newEventWithMockedServices = {
		val event = new SmallGroupEvent()
		event.smallGroupService = None
		event.permissionsService = mock[PermissionsService]
		event
	}

  @Test
  def hasEquivalentEventsToReturnsTrueForSameGroup() {
    val group = newSmallGroupWithMockedServices

    group.hasEquivalentEventsTo(group) should be(true)
  }

  @Test
  def hasEquivalentEventsToReturnsTrueForGroupsWithNoEvents(){
    val group = newSmallGroupWithMockedServices
    group.hasEquivalentEventsTo(newSmallGroupWithMockedServices) should be (true)
  }

  @Test
  def hasEquivalentEventsToReturnsTrueForGroupsWithEquivalentEvents(){

    val group = newSmallGroupWithMockedServices
    group.events = JArrayList(event)
    val group2 = newSmallGroupWithMockedServices
    group2.events = JArrayList(equivalentEvent)
    group.hasEquivalentEventsTo(group2) should be (true)
  }

  @Test
  def hasEquivalentEventsToReturnsFalseForGroupsWithNonEquivalentEvents(){
    val group = newSmallGroupWithMockedServices
    group.events = JArrayList(event)
    val group2 = newSmallGroupWithMockedServices
    group2.events = JArrayList(notEquivalentEvent)
    group.hasEquivalentEventsTo(group2) should be (false)
  }

  @Test
  def hasEquivalentEventsToReturnsFalseForGroupsWithSubsetOfEvents(){
    val group = newSmallGroupWithMockedServices
    group.events = JArrayList(event)
    val group2 = newSmallGroupWithMockedServices
    group2.events = JArrayList(event,notEquivalentEvent)
    group.hasEquivalentEventsTo(group2) should be (false)
  }

  @Test
  def duplicateCopiesAllFields(){

    val source = newSmallGroupWithMockedServices
    val clonedEvent = newEventWithMockedServices

    // can't use a mockito mock because the final equals method on GeneratedId causes mockito to
    // blow up
    val event:SmallGroupEvent = new SmallGroupEvent{
      override def duplicateTo(g:SmallGroup) = clonedEvent
    }

    val sourceSet = new SmallGroupSet
    sourceSet.defaultMaxGroupSize = 11
    sourceSet.defaultMaxGroupSizeEnabled = true

    source.name = "name"
    source.groupSet = sourceSet
    source.permissionsService = mock[PermissionsService]

		// would be nice to use a mock here, but they don't work well with GeneratedId classes
		val studentsGroup = UserGroup.ofUniversityIds
		studentsGroup.addUserId("test user")
    source.students = studentsGroup

    source.deleted = false
    source.id = "123"
    source.events = JArrayList(event)
    source.maxGroupSize = 12

    val targetSet = new SmallGroupSet
    targetSet.defaultMaxGroupSize = 10
    targetSet.defaultMaxGroupSizeEnabled = true

    val target = source.duplicateTo(targetSet )

    // id is not copeied, otherwise it wouldn't be transient
    target.id should be(source.id)
    target.name should be(source.name)
    target.groupSet should not be(source.groupSet)
    target.groupSet should be(targetSet)
    target.maxGroupSize should be(source.maxGroupSize)

    target.permissionsService should be(source.permissionsService)
    target.students should not be(source.students)
		target.students.hasSameMembersAs(source.students) should be(true)
    target.events.size should be(1)
    target.events.asScala.head should be(clonedEvent)

  }

	@Test
	def isFullReportsFullnessWhenGroupSizeLimitsEnabled(){

		// set up a group with 1 member, with group size limits enabled
		val group = newSmallGroupWithMockedServices
		group.groupSet = new SmallGroupSet()
		group.groupSet.defaultMaxGroupSizeEnabled = true
		group.students.add(new User("test") {{ setWarwickId("00000001") }})


		group.maxGroupSize = 2
		group should not be('full)

		group.maxGroupSize = 1
		group should be('full)

		group.maxGroupSize = 0
		group should be('full)

	}

	@Test
	def isFullIsFalseWhenGroupSizeLimitsNotEnabled(){

		// set up a group with 1 member, with group size limits enabled
		val group = newSmallGroupWithMockedServices
		group.groupSet = new SmallGroupSet()
		group.groupSet.defaultMaxGroupSizeEnabled = false
		group.students.add(new User("test"))


		group.maxGroupSize = 2
		group should not be('full)

		group.maxGroupSize = 1
		group should not be('full)

		group.maxGroupSize = 0
		group should not be('full)
	}
}
