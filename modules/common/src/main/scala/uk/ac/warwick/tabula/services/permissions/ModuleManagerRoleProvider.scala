package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._

import org.springframework.stereotype.Component

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.ModuleManager
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

@Component
class ModuleManagerRoleProvider extends RoleProvider {
	
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] =
		scope match {
			case department: Department => 
				department.modules filter {_.ensuredParticipants.includes(user.idForPermissions)} map {ModuleManager(_)}
			
			case module: Module =>
				if (module.ensuredParticipants.includes(user.idForPermissions)) Seq(ModuleManager(module))
				else Seq()
		}
	
	def rolesProvided = Set(classOf[ModuleManager])
	
}