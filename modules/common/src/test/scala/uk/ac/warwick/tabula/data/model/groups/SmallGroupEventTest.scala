package uk.ac.warwick.tabula.data.model.groups

import org.junit.Test
import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.data.model.UserGroup

class SmallGroupEventTest extends TestBase with Mockito{

  @Test
  def canDuplicate(){
    val source = new SmallGroupEvent
    source.id = "testId"
    source.day = DayOfWeek.Monday
    source.endTime = new LocalTime(13,0,0,0)
    source.group = new SmallGroup
    source.location = "test"
    source.permissionsService = mock[PermissionsService]
    source.startTime = new LocalTime(12,0,0,0)
    source.title= "test title"
    val clonedTutors = new UserGroup
    source.tutors = new UserGroup{
      override def duplicate() = clonedTutors
    }
    source.weekRanges = Seq(WeekRange(1))

    val targetGroup = new SmallGroup()
    val clone = source.duplicateTo(targetGroup)

    clone.id should be(source.id)
    clone.day should be (source.day)
    clone.endTime should be(source.endTime)
    clone.group should be(targetGroup)
    clone.location should be(source.location)
    clone.permissionsService should be(source.permissionsService)
    clone.startTime should be(source.startTime)
    clone.title should be(source.title)
    clone.tutors should be(clonedTutors)
    clone.weekRanges should be(source.weekRanges)


  }

}
