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
import uk.ac.warwick.tabula.roles.StudentRole
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class UserTypeAndDepartmentRoleProvider extends ScopelessRoleProvider with TaskBenchmarking {
	
	var profileService = Wire.auto[ProfileService]
	val departmentService = promise { Wire[ModuleAndDepartmentService] }
	
	private def getRolesForMembers(members: Seq[Member]): Stream[Role] = members.toStream.flatMap { member =>
		val memberRole = customRoleFor(Option(member.homeDepartment))(UniversityMemberRoleDefinition, member).getOrElse(UniversityMemberRole(member))
		
		memberRole #:: (member.userType match {
			case Student => member.touchedDepartments.map { department => 
				customRoleFor(department)(StudentRoleDefinition, department).getOrElse(StudentRole(department)) 
			}
			case Staff => member.affiliatedDepartments.map { department => 
				customRoleFor(department)(StaffRoleDefinition, department).getOrElse(StaffRole(department)) 
			}
			case Emeritus => member.affiliatedDepartments.map { department => 
				customRoleFor(department)(StaffRoleDefinition, department).getOrElse(StaffRole(department)) 
			}
			case _ => Stream.empty
		})
	}

	/**
	 * In the case of users not having a member record, we give out definitions based on their SSO
	 * attributes. This is potentially leaky in terms of PGRs, who would normally get StudentRoleDefinition
	 * but will in fact get SSOStaffRoleDefinition instead; the reason for SSOStaffRole existing is
	 * to make sure we don't expose anything truly sensitive in it.
	 */
	private def getRolesForSSO(user: CurrentUser) =
		if (user.departmentCode.hasText) {
			departmentService.get.getDepartmentByCode(user.departmentCode.toLowerCase) match {
				case Some(department) =>
					if (user.isStaff) Stream(customRoleFor(department)(SSOStaffRoleDefinition, department).getOrElse(SSOStaffRole(department)))
					else if (user.isStudent) Stream(customRoleFor(department)(StudentRoleDefinition, department).getOrElse(StudentRole(department)))
					else Stream.empty
				case None => Stream.empty
			}
		}
		else Stream.empty

	def getRolesFor(user: CurrentUser): Stream[Role] = benchmarkTask("Get roles for UserTypeAndDepartmentRoleProvider") {
		if (user.realUser.isLoggedIn) {
			val members = profileService.getAllMembersWithUserId(user.apparentId, disableFilter = true)

			if (members.nonEmpty) getRolesForMembers(members) :+ LoggedInRole(user.apparentUser)
			else getRolesForSSO(user) :+ LoggedInRole(user.apparentUser)
		} else Stream.empty
	}
	
	def rolesProvided = Set(classOf[StudentRole], classOf[StaffRole], classOf[UniversityMemberRole])
	
}