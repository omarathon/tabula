package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._

import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.roles.Submitter

@Component
class SubmitterRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] = {		
		scope match {
			case submission: Submission => 
				if (submission.universityId == user.universityId) Seq(Submitter(submission))
				else Seq()
			
			// Submitter is only checked at the assignment level
			case _ => Seq()
		}
	}
	
	def rolesProvided = Set(classOf[Submitter])
	
}