package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.helpers.RequestLevelCaching
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{Marker, MarkerRoleDefinition, Role}
import uk.ac.warwick.tabula.services.AssignmentService

@Component
class MarkerRoleProvider extends RoleProvider with TaskBenchmarking with RequestLevelCaching[(CurrentUser, String), Seq[Assignment]] {

	val assignmentService = promise { Wire[AssignmentService] }
	
	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for MarkerRoleProvider") {
		def getRoles(assignmentsForMarker: Stream[Assignment]) = assignmentsForMarker.map{ assignment =>
			customRoleFor(assignment.module.adminDepartment)(MarkerRoleDefinition, assignment).getOrElse(Marker(assignment))
		} 
		
		scope match {
			case department: Department =>
				getRoles(cachedBy((user, scope.toString)) {
					assignmentService.get.getAssignmentsByDepartmentAndMarker(department, user)
				}.toStream)
			
			case module: Module =>
				getRoles(cachedBy((user, scope.toString)) {
					assignmentService.get.getAssignmentsByModuleAndMarker(module, user).toStream
				}.toStream)

			case assignment: Assignment if assignment.isMarker(user.apparentUser) =>
				getRoles(Stream(assignment))

			// We don't need to check for the marker role on any other scopes
			case _ => Stream.empty
		}
	}
	
	def rolesProvided = Set(classOf[Marker])
	
}