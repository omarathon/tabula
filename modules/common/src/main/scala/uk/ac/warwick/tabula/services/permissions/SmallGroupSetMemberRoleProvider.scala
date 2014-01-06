package uk.ac.warwick.tabula.services.permissions


import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{Role, SmallGroupSetMember, SmallGroupSetViewer}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.roles.SmallGroupSetMemberRoleDefinition
import uk.ac.warwick.tabula.roles.SmallGroupSetViewerRoleDefinition

@Component
class SmallGroupSetMemberRoleProvider extends RoleProvider {

	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget) = scope match {
		case set: SmallGroupSet => getRoles(user, Seq(set))
		case _ => Stream.empty
	}

	private def getRoles(user: CurrentUser, sets: Seq[SmallGroupSet]) = {
		val memberSets =
			sets.toStream
		  .filter { _.isStudentMember(user.apparentUser) }
		  .distinct

		val memberRoles: Stream[Role] = memberSets.map { set => 
			customRoleFor(set.module.department)(SmallGroupSetMemberRoleDefinition, set).getOrElse(SmallGroupSetMember(set)) 
		}
		val viewerRoles: Stream[Role] = memberSets.filter { _.studentsCanSeeOtherMembers }.map { set => 
			customRoleFor(set.module.department)(SmallGroupSetViewerRoleDefinition, set).getOrElse(SmallGroupSetViewer(set)) 
		}

		memberRoles #::: viewerRoles
	}
	
	def rolesProvided = Set(classOf[SmallGroupSetMember], classOf[SmallGroupSetViewer])
	
}