package uk.ac.warwick.tabula.groups.controllers

import org.mockito.Mockito._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.groups.SmallGroupFixture
import uk.ac.warwick.tabula.groups.commands.admin.{AdminSmallGroupsHomeCommandState, AdminSmallGroupsHomeInformation}
import uk.ac.warwick.tabula.groups.web.controllers.admin.AdminDepartmentHomeController
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewSetWithProgress
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}

import scala.collection.immutable.ListMap

class AdminDepartmentHomeControllerTest extends TestBase with Mockito{

  def createController = {
    val controller = new AdminDepartmentHomeController()
    controller.securityService = mock[SecurityService]
    when(controller.securityService.can(any[CurrentUser], any[Permission], any[PermissionsTarget])).thenReturn{true}
    controller
  }

  @Test
  def reportsWhenNotAllGroupsetsAreReleased(){new SmallGroupFixture {
    withUser("test") {
      groupSet1.releasedToStudents = true
      groupSet1.releasedToTutors = false

      val cmd = mock[Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState]
      when(cmd.apply()).thenReturn(AdminSmallGroupsHomeInformation(false, Seq(groupSet1.module), Seq(ViewSetWithProgress(groupSet1, Nil, GroupsViewModel.Tutor, null, None, ListMap())), Nil))
      val mav = createController.adminDepartment(cmd, department, currentUser)
      mav.map.get("hasUnreleasedGroupsets") match{
        case Some(v: Boolean) => v should be {true}
        case _ => fail()
      }
    }}
  }

  @Test
  def reportsWhenAllGroupsetsAreReleased(){new SmallGroupFixture {
    withUser("test"){

      groupSet1.releasedToStudents = true
      groupSet1.releasedToTutors = true

      val cmd = mock[Appliable[AdminSmallGroupsHomeInformation] with AdminSmallGroupsHomeCommandState]
      when(cmd.apply()).thenReturn(AdminSmallGroupsHomeInformation(false, Seq(groupSet1.module), Seq(ViewSetWithProgress(groupSet1, Nil, GroupsViewModel.Tutor, null, None, ListMap())), Nil))
      val mav = createController.adminDepartment(cmd, department, currentUser)
      mav.map.get("hasUnreleasedGroupsets") match {
        case Some(v: Boolean) => v should be {false}
        case _ => fail()
      }

    }}

  }

}
