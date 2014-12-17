package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.SmallGroupTutor
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.roles.SmallGroupTutorRoleDefinition
import uk.ac.warwick.tabula.commands.TaskBenchmarking

@Component
class SmallGroupTutorRoleProvider extends RoleProvider with TaskBenchmarking {

	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget) = benchmarkTask("Get roles for SmallGroupTutorRoleProvider") {
		scope match {
			case event: SmallGroupEvent => getRoles(user, Seq(event))
			case group: SmallGroup => getRoles(user, group.events)
			case _ => Stream.empty
		}
	}

	private def getRoles(user: CurrentUser, events: Seq[SmallGroupEvent]) =
		events.toStream
		  .filter { _.tutors.includesUser(user.apparentUser) }
		  .map { _.group }
			.filter { _.groupSet.releasedToTutors }
		  .distinct
		  .map { group =>
		 	  customRoleFor(group.groupSet.module.adminDepartment)(SmallGroupTutorRoleDefinition, group).getOrElse(SmallGroupTutor(group))
		 	}
	
	def rolesProvided = Set(classOf[SmallGroupTutor])
	
}