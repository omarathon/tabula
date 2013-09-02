package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions

case class SmallGroupSetMember(smallGroupSet:SmallGroupSet) extends BuiltInRole(SmallGroupSetMemberRoleDefinition, smallGroupSet)

case object SmallGroupSetMemberRoleDefinition extends BuiltInRoleDefinition {

	override def description = "SmallGroupSetMember"

	GrantsScopedPermission(
		Permissions.SmallGroups.AllocateSelf
	)

}