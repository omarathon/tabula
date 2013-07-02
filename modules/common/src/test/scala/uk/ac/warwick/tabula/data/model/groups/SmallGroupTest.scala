package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.{Mockito, TestBase}
import junit.framework.Test
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.data.model.UserGroup
import org.mockito.Mockito._
import org.mockito.Matchers.anyObject
import scala.collection.JavaConverters._

class SmallGroupTest extends TestBase with Mockito {

  val event = new SmallGroupEvent()
  val equivalentEvent = new SmallGroupEvent()
  val notEquivalentEvent = new SmallGroupEvent()
  notEquivalentEvent.day = DayOfWeek.Monday

  @Test
  def hasEquivalentEventsToReturnsTrueForSameGroup() {
    val group = new SmallGroup()
    group.hasEquivalentEventsTo(group) should be(true)
  }

  @Test
  def hasEquivalentEventsToReturnsTrueForGroupsWithNoEvents(){
    val group = new SmallGroup()
    group.hasEquivalentEventsTo(new SmallGroup()) should be (true)
  }

  @Test
  def hasEquivalentEventsToReturnsTrueForGroupsWithEquivalentEvents(){

    val group = new SmallGroup()
    group.events = JArrayList(event)
    val group2 = new SmallGroup()
    group2.events = JArrayList(equivalentEvent)
    group.hasEquivalentEventsTo(group2) should be (true)
  }

  @Test
  def hasEquivalentEventsToReturnsFalseForGroupsWithNonEquivalentEvents(){
    val group = new SmallGroup()
    group.events = JArrayList(event)
    val group2 = new SmallGroup()
    group2.events = JArrayList(notEquivalentEvent)
    group.hasEquivalentEventsTo(group2) should be (false)
  }

  @Test
  def hasEquivalentEventsToReturnsFalseForGroupsWithSubsetOfEvents(){
    val group = new SmallGroup()
    group.events = JArrayList(event)
    val group2 = new SmallGroup()
    group2.events = JArrayList(event,notEquivalentEvent)
    group.hasEquivalentEventsTo(group2) should be (false)
  }

  @Test
  def duplicateCopiesAllFields(){

    val source = new SmallGroup
    val clonedEvent = new SmallGroupEvent
    // can't use a mockito mock because the final equals method on GeneratedId causes mockito to
    // blow up
    val event:SmallGroupEvent = new SmallGroupEvent{
      override def duplicateTo(g:SmallGroup) = clonedEvent
    }

    val sourceSet = new SmallGroupSet

    source.name = "name"
    source.groupSet = sourceSet
    source.permissionsService = mock[PermissionsService]
    source.students = mock[UserGroup]
    source.deleted = false
    source.id = "123"
    source.events = JArrayList(event)

    val targetSet = new SmallGroupSet

    val target = source.duplicateTo(targetSet )

    // id is not copeied, otherwise it wouldn't be transient
    target.id should be(source.id)
    target.name should be(source.name)
    target.groupSet should not be(source.groupSet)
    target.groupSet should be(targetSet)

    target.permissionsService should be(source.permissionsService)
    target.students should not be(source.students)
    verify(source.students, times(1)).duplicate()

    target.events.size should be(1)
    target.events.asScala.head should be(clonedEvent)

  }

}
