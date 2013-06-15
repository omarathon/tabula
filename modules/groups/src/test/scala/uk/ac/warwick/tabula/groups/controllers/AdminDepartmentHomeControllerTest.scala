package uk.ac.warwick.tabula.groups.controllers

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import junit.framework.Test
import uk.ac.warwick.tabula.groups.web.controllers.admin.{AdminDepartmentHomeController, AdminDepartmentHomeCommand}
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import scala.collection.JavaConverters._
import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.Appliable

class AdminDepartmentHomeControllerTest extends TestBase with Mockito{


  @Test
  def reportsWhenNotAllGroupsetsAreReleased(){new SmallGroupFixture {
    withUser("test"){

      groupSet1.releasedToStudents = true
      groupSet1.releasedToTutors = false

      val cmd = mock[Appliable[Seq[Module]]]
      when(cmd.apply()).thenReturn(Seq(groupSet1.module))
      val mav = new AdminDepartmentHomeController().adminDepartment(cmd,department)
      mav.map.get("hasUnreleasedGroupsets") should be(Some(true))
    }}


  }

  @Test
  def reportsWhenAllGroupsetsAreReleased(){new SmallGroupFixture {
    withUser("test"){

      groupSet1.releasedToStudents = true
      groupSet1.releasedToTutors = true

      val cmd = mock[Appliable[Seq[Module]]]
      when(cmd.apply()).thenReturn(Seq(groupSet1.module))
      val mav = new AdminDepartmentHomeController().adminDepartment(cmd, department)
      mav.map.get("hasUnreleasedGroupsets") should be(Some(false))
    }}

  }

}
