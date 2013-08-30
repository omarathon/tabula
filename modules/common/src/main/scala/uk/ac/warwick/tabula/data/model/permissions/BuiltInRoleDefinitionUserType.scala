package uk.ac.warwick.tabula.data.model.permissions

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import uk.ac.warwick.tabula.roles.RoleDefinition
import java.sql.Types
import uk.ac.warwick.tabula.roles.SelectorBuiltInRoleDefinition
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Promises._

class BuiltInRoleDefinitionUserType extends AbstractBasicUserType[BuiltInRoleDefinition, String] {
	
	val relationshipService = promise { Wire[RelationshipService] }

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null
	
	implicit class Regex(sc: StringContext) {
		def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
	}

	override def convertToObject(legacy: String) = {
		// Hardcoded legacy code. Ew
		val string = legacy match {
			case "PersonalTutorRoleDefinition" => "StudentRelationshipAgentRoleDefinition(tutor)"
			case "SupervisorRoleDefinition" => "StudentRelationshipAgentRoleDefinition(supervisor)"
			case _ => legacy
		}
		
		string match {
			case r"([A-Za-z]+)${roleName}\(([^\)]+)${id}\)" => {
				val selector = relationshipService.get.getStudentRelationshipTypeByUrlPart(id) match {
					case Some(selector) => selector
					case _ => relationshipService.get.getStudentRelationshipTypeById(id).get // Fall back to ID, just in case
				}
				
				SelectorBuiltInRoleDefinition.of(roleName, selector) // FIXME hard-wired
			}
			case _ => RoleDefinition.of(string)
		}
	}
	
	override def convertToValue(definition: BuiltInRoleDefinition) = definition match {
		case defn: SelectorBuiltInRoleDefinition[_] => "%s(%s)".format(defn.getName, defn.selector.id)
		case _ => definition.getName
	}

}