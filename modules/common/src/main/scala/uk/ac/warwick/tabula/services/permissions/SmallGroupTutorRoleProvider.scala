package uk.ac.warwick.tabula.services.permissions

import scala.collection.JavaConverters._

import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{SmallGroupTutor, Marker, Role}
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}

@Component
class SmallGroupTutorRoleProvider extends RoleProvider {

	var smallGroupService = Wire[SmallGroupService]
	
	override def getRolesFor(user: CurrentUser, scope: PermissionsTarget) = scope match {
		case event: SmallGroupEvent => getRoles(user, Seq(event))
		case group: SmallGroup => getRoles(user, group.events.asScala)
		case _ => Stream.empty
	}

	private def getRoles(user: CurrentUser, events: Seq[SmallGroupEvent]) =
		events.toStream
		  .filter { _.tutors.includes(user.apparentId) }
		  .map { _.group }
		  .distinct
		  .map { SmallGroupTutor(_) }
	
	def rolesProvided = Set(classOf[Marker])
	
}