package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Component
class DepartmentalAdministratorRoleProvider extends RoleProvider {
	
	var departmentService = Wire.auto[ModuleAndDepartmentService]

	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] =
		scope match {
			case department: Department => 
				if (department.isOwnedBy(user.idForPermissions)) Seq(DepartmentalAdministrator(department))
				else Seq()
				
			case _ => Seq()
		}
	
	def rolesProvided = Set(classOf[DepartmentalAdministrator])
	
}