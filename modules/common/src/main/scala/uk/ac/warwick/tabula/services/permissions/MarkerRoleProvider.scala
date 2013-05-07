package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._



import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Marker
import uk.ac.warwick.tabula.roles.Role

@Component
class MarkerRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = {
		def getRoles(assignments: Seq[Assignment]) = assignments.toStream filter { _.isMarker(user.apparentUser) } map {Marker(_)} 
		
		scope match {
			case department: Department => 
				getRoles(department.modules flatMap { _.assignments })
			
			case module: Module =>
				getRoles(module.assignments)
				
			case assignment: Assignment =>
				getRoles(Seq(assignment))
				
			// We don't need to check for the marker role on any other scopes
			case _ => Stream.empty
		}
	}
	
	def rolesProvided = Set(classOf[Marker])
	
}