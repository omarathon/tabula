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
import uk.ac.warwick.tabula.roles.StudentRelationshipAgentRoleDefinition

@Component
class StudentRelationshipAgentRoleProvider extends RoleProvider {

	var relationshipService = Wire[RelationshipService]

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = scope match {
		case member: model.Member => {
			relationshipService
				.listAllStudentRelationshipsWithUniversityId(user.universityId)
				.toStream
				.filter { _.studentId == member.universityId }
				.flatMap { rel => rel.studentMember.map { student => (rel.relationshipType, student) } }
				.distinct
				.map { case (relationshipType, student) => 
					/*
					 * Check the student department for custom roles only, not the agent department,
					 * as that's what we're performing operations on.
					 */
					val studentDepartment = 
						student.mostSignificantCourseDetails
							.toSeq
							.flatMap { scd =>
								Option(scd.latestStudentCourseYearDetails.enrolmentDepartment).toSeq ++ Option(scd.route).flatMap { r => Option(r.department) }.toSeq
							}

					studentDepartment
						.flatMap { customRoleFor(_)(StudentRelationshipAgentRoleDefinition(relationshipType), member) }
						.headOption
						.getOrElse { StudentRelationshipAgent(member, relationshipType) }
				}
		}

		// We don't need to check for the StudentRelationshipAgent role on any other scopes
		case _ => Stream.empty
	}

	def rolesProvided = Set(classOf[StudentRelationshipAgent])
	
}