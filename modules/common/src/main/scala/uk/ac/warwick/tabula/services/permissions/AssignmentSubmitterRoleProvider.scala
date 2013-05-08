package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._

import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.AssignmentSubmitter
import uk.ac.warwick.tabula.roles.Role

@Component
class AssignmentSubmitterRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = {
		scope match {
			case assignment: Assignment => 
				if (assignment.canSubmit(user.apparentUser)) Stream(AssignmentSubmitter(assignment))
				else Stream.empty
			
			// AssignmentSubmitter is only checked at the assignment level
			case _ => Stream.empty
		}
	}
	
	def rolesProvided = Set(classOf[AssignmentSubmitter])
	
}