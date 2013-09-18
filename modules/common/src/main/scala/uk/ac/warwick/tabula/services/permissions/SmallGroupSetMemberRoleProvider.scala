package uk.ac.warwick.tabula.services.permissions


import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{Role, SmallGroupSetMember, SmallGroupSetViewer}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

@Component
class SmallGroupSetMemberRoleProvider extends RoleProvider {

	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget) = scope match {
		case set: SmallGroupSet => getRoles(user, Seq(set))
		case _ => Stream.empty
	}

	private def getRoles(user: CurrentUser, sets: Seq[SmallGroupSet]) = {
		val memberSets =
			sets.toStream
		  .filter { _.members.includesUser(user.apparentUser) }
		  .distinct

		val memberRoles: Stream[Role] = memberSets.map { SmallGroupSetMember(_) }
		val viewerRoles: Stream[Role] = memberSets.filter { _.studentsCanSeeOtherMembers }.map { SmallGroupSetViewer(_) }

		memberRoles #::: viewerRoles
	}
	
	def rolesProvided = Set(classOf[SmallGroupSetMember], classOf[SmallGroupSetViewer])
	
}