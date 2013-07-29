package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup

case class SmallGroupTutor(smallGroup: SmallGroup) extends BuiltInRole(SmallGroupTutorRoleDefinition, smallGroup)

case object SmallGroupTutorRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "SmallGroupTutor"
	
	GrantsScopedPermission(
		SmallGroups.Read,
		SmallGroupEvents.Register
	)

}