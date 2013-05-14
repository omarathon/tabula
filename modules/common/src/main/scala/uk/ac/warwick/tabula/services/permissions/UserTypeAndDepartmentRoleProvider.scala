package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.StaffRole
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.roles.UniversityMemberRole
import uk.ac.warwick.tabula.roles.StudentRole
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Promises._

@Component
class UserTypeAndDepartmentRoleProvider extends ScopelessRoleProvider {
	
	var profileService = Wire.auto[ProfileService]
	val departmentService = promise { Wire[ModuleAndDepartmentService] }
	
	private def getRolesForMembers(members: Seq[Member]) = members.toStream flatMap { member =>
		UniversityMemberRole(member) #:: (member.userType match {
			case Student => member.touchedDepartments map { StudentRole(_) }
			case Staff => member.affiliatedDepartments map { StaffRole(_) }
			case Emeritus => member.affiliatedDepartments map { StaffRole(_) }
			case _ => Stream.empty
		})
	}
	
	private def getRolesForSSO(user: CurrentUser) =
		if (user.departmentCode.hasText) {
			departmentService.get.getDepartmentByCode(user.departmentCode.toLowerCase) match {
				case Some(department) =>
					if (user.isStaff) Stream(StaffRole(department))
					else if (user.isStudent) Stream(StudentRole(department))
					else Stream.empty
				case None => Stream.empty
			}
		}
		else Stream.empty

	def getRolesFor(user: CurrentUser): Stream[Role] = {
		if (user.realUser.isLoggedIn) {
			val members = profileService.getAllMembersWithUserId(user.apparentId, true)
			if (!members.isEmpty) getRolesForMembers(members)
			else getRolesForSSO(user)
		} else Stream.empty
	}
	
	def rolesProvided = Set(classOf[StudentRole], classOf[StaffRole], classOf[UniversityMemberRole])
	
}