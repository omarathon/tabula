package uk.ac.warwick.tabula.groups.controllers

import uk.ac.warwick.tabula.{Mockito, CurrentUser, TestBase}
import uk.ac.warwick.tabula.groups.web.controllers.admin.{AdminDepartmentHomeController}
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules

class AdminDepartmentHomeControllerTest extends TestBase with Mockito{


  def createController = {
    val controller = new AdminDepartmentHomeController()
    controller.securityService = mock[SecurityService]
    when(controller.securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget])).thenReturn(true)
    controller
  }
  @Test
  def reportsWhenNotAllGroupsetsAreReleased(){new SmallGroupFixture {
    withUser("test"){

      groupSet1.releasedToStudents = true
      groupSet1.releasedToTutors = false

      val cmd = mock[Appliable[Seq[Module]]]
      when(cmd.apply()).thenReturn(Seq(groupSet1.module))
      val mav = createController.adminDepartment(cmd,department, currentUser)
      mav.map.get("data") match{
        case Some(v:ViewModules) => v.hasUnreleasedGroupsets() should be(true)
        case _ => fail()
      }
    }}


  }

  @Test
  def reportsWhenAllGroupsetsAreReleased(){new SmallGroupFixture {
    withUser("test"){

      groupSet1.releasedToStudents = true
      groupSet1.releasedToTutors = true

      val cmd = mock[Appliable[Seq[Module]]]
      when(cmd.apply()).thenReturn(Seq(groupSet1.module))
      val mav = createController.adminDepartment(cmd, department, currentUser)
      mav.map.get("data") match{
        case Some(v:ViewModules) => v.hasUnreleasedGroupsets() should be(false)
        case _ => fail()
      }

    }}

  }

}
