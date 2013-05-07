package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.roles.PersonalTutor
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire


@Component
class PersonalTutorRoleProvider extends RoleProvider {
	
	var profileService = Wire.auto[ProfileService]

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = scope match {
		case member: model.Member => {
			val tuteeIds = 
				profileService.listStudentRelationshipsWithUniversityId(model.RelationshipType.PersonalTutor, user.universityId).map { _.studentId }.toStream
			if (tuteeIds.contains(member.universityId))
				Stream(PersonalTutor(member))
			else
				Stream.empty
		}
			
		// We don't need to check for the PersonalTutor role on any other scopes
		case _ => Stream.empty
	}
	
	
	def rolesProvided = Set(classOf[PersonalTutor])
}