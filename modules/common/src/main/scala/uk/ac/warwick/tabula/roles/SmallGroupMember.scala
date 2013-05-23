package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._

case class SmallGroupMember(group: model.groups.SmallGroup) extends BuiltInRole(SmallGroupMemberRoleDefinition, group)

object SmallGroupMemberRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Small Group Member"

}
