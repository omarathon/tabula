package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.roles.StudentRelationshipAgent

@Component
class StudentRelationshipAgentRoleProvider extends RoleProvider {

	var relationshipService = Wire[RelationshipService]

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = scope match {
		case member: model.Member => {
			relationshipService
				.listAllStudentRelationshipsWithUniversityId(user.universityId)
				.toStream
				.filter { _.studentId == member.universityId }
				.map { rel => 
					StudentRelationshipAgent(member, rel.relationshipType)
				}
		}

		// We don't need to check for the StudentRelationshipAgent role on any other scopes
		case _ => Stream.empty
	}

	def rolesProvided = Set(classOf[StudentRelationshipAgent])
	
}