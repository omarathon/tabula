package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.roles.UniversityMemberRole
import uk.ac.warwick.tabula.roles.StaffRole
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.helpers.MutablePromise

@Component
class UserTypeAndDepartmentRoleProvider extends ScopelessRoleProvider with TaskBenchmarking {

  var profileService: ProfileService = Wire.auto[ProfileService]
  val departmentService: MutablePromise[ModuleAndDepartmentService] = promise {
    Wire[ModuleAndDepartmentService]
  }

  private def getRolesForMembers(members: Seq[Member]): LazyList[Role] = members.to(LazyList).flatMap { member =>
    if (member.active) {
      val memberRole = customRoleFor(Option(member.homeDepartment))(UniversityMemberRoleDefinition, member).getOrElse(UniversityMemberRole(member))

      memberRole #:: (member.userType match {
        case Staff | Emeritus => member.affiliatedDepartments.map { department =>
          customRoleFor(department)(StaffRoleDefinition, department).getOrElse(StaffRole(department))
        }
        case _ => LazyList.empty
      })
    } else {
      LazyList(customRoleFor(Option(member.homeDepartment))(PreviousUniversityMemberRoleDefinition, member).getOrElse(PreviousUniversityMemberRole(member)))
    }
  }

  /**
    * In the case of users not having a member record, we give out definitions based on their SSO
    * attributes. This is potentially leaky in terms of PGRs, who will get SSOStaffRoleDefinition;
    * the reason for SSOStaffRole existing is to make sure we don't expose anything truly sensitive in it.
    */
  private def getRolesForSSO(user: CurrentUser) =
    if (user.departmentCode.hasText) {
      departmentService.get.getDepartmentByCode(user.departmentCode.toLowerCase) match {
        case Some(department) =>
          if (user.isStaff) LazyList(customRoleFor(department)(SSOStaffRoleDefinition, department).getOrElse(SSOStaffRole(department)))
          else LazyList.empty
        case None => LazyList.empty
      }
    }
    else LazyList.empty

  def getRolesFor(user: CurrentUser): LazyList[Role] = benchmarkTask("Get roles for UserTypeAndDepartmentRoleProvider") {
    if (user.realUser.isLoggedIn) {
      val members = profileService.getAllMembersWithUserId(user.apparentId, disableFilter = true, activeOnly = false)

      if (members.nonEmpty) getRolesForMembers(members) :+ LoggedInRole(user.apparentUser)
      else getRolesForSSO(user) :+ LoggedInRole(user.apparentUser)
    } else LazyList.empty
  }

  def rolesProvided = Set(classOf[StaffRole], classOf[UniversityMemberRole])

}
