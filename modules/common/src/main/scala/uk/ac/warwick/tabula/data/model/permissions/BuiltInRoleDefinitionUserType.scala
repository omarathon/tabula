package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.roles.RoleDefinition
import java.sql.Types

class BuiltInRoleDefinitionUserType extends AbstractBasicUserType[BuiltInRoleDefinition, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = RoleDefinition.of(string)	
	override def convertToValue(definition: BuiltInRoleDefinition) = definition.getName

}