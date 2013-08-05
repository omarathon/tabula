package uk.ac.warwick.tabula.data.model.groups

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
    source.tutors = UserGroup.ofUsercodes
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
    clone.tutors should not be(source.tutors) // just check they're different instances
    clone.weekRanges should be(source.weekRanges)


  }

}
