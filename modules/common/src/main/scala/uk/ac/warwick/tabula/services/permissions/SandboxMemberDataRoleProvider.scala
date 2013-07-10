package uk.ac.warwick.tabula.services.permissions

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.roles.UniversityMemberRole
import uk.ac.warwick.tabula.roles.StudentRole
import uk.ac.warwick.tabula.roles.StaffRole
import uk.ac.warwick.tabula.roles.UniversityMemberRole
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.RuntimeMember
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator

/**
 * Role provider that only runs in the sandbox environment, where there are no 
 * member details for the currently logged in user.
 */
@Profile(Array("sandbox")) @Component
class SandboxMemberDataRoleProvider extends ScopelessRoleProvider {
	
	val departmentService = promise { Wire[ModuleAndDepartmentService] }
	
	def getRolesFor(user: CurrentUser): Stream[Role] = {
		if (user.realUser.isLoggedIn) {
			val allDepartments = departmentService.get.allDepartments.toStream
			
			val member = new RuntimeMember(user) {
				this.homeDepartment = allDepartments.head
				override def affiliatedDepartments = allDepartments
				override def touchedDepartments = allDepartments
			}
			
			UniversityMemberRole(member) #:: (member.userType match {
				case Student => allDepartments map { StudentRole(_) }
				case Staff | Emeritus => allDepartments map { StaffRole(_) }
				case _ => Stream.empty
			})
		} else Stream.empty
	}
	
	def rolesProvided = Set(classOf[StudentRole], classOf[StaffRole], classOf[UniversityMemberRole])

}