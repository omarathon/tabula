package uk.ac.warwick.tabula.services.permissions

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.roles.StaffRole
import uk.ac.warwick.tabula.roles.UniversityMemberRole
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, RuntimeMember}
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.helpers.MutablePromise

/**
  * Role provider that only runs in the sandbox environment, where there are no
  * member details for the currently logged in user.
  */
@Profile(Array("sandbox"))
@Component
class SandboxMemberDataRoleProvider extends ScopelessRoleProvider with TaskBenchmarking {

  val departmentService: MutablePromise[ModuleAndDepartmentService] = promise {
    Wire[ModuleAndDepartmentService]
  }

  def getRolesFor(user: CurrentUser): LazyList[Role] = benchmarkTask("Get roles for SandboxMemberDataRoleProvider") {
    if (user.realUser.isLoggedIn) {
      val allDepartments = departmentService.get.allDepartments.to(LazyList)

      val member = new RuntimeMember(user) {
        allDepartments.headOption.foreach(this.homeDepartment = _)

        override def affiliatedDepartments: LazyList[Department] = allDepartments

        override def touchedDepartments: LazyList[Department] = allDepartments
      }

      UniversityMemberRole(member) #:: (member.userType match {
        case Staff | Emeritus => allDepartments map StaffRole
        case _ => LazyList.empty[Role]
      })
    } else LazyList.empty
  }

  def rolesProvided = Set(classOf[StaffRole], classOf[UniversityMemberRole])

}
