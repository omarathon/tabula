package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.data.Daoisms

class RoleDefinitionConverter extends TwoWayConverter[String, RoleDefinition] with Daoisms {
	
	override def convertLeft(definition: RoleDefinition) = Option(definition) match {
		case Some(builtIn: BuiltInRoleDefinition) => builtIn.getName
		case Some(custom: CustomRoleDefinition) => custom.getId
		case None => null
	}
  
	override def convertRight(name: String) = 
		if (!name.hasText) null
		else getById[CustomRoleDefinition](name) getOrElse RoleDefinition.of(name)

}