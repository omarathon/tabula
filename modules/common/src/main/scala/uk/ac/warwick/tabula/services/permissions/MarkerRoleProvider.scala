package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.roles.ModuleManager
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.roles.Marker
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Component
class MarkerRoleProvider extends RoleProvider {
	
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] = {
		def getRoles(assignments: Seq[Assignment]) = assignments filter { _.isMarker(user.apparentUser) } map {Marker(_)} 
		
		scope match {
			case department: Department => 
				getRoles(department.modules flatMap { _.assignments })
			
			case module: Module =>
				getRoles(module.assignments)
				
			case assignment: Assignment =>
				getRoles(Seq(assignment))
				
			// We don't need to check for the marker role on any other scopes
			case _ => Seq()
		}
	}
	
	def rolesProvided = Set(classOf[Marker])
	
}