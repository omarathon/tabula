package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._

case class SmallGroupTutor(group: model.groups.SmallGroup) extends BuiltInRole(SmallGroupTutorRoleDefinition, group)

object SmallGroupTutorRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Small Group Tutor"

}
