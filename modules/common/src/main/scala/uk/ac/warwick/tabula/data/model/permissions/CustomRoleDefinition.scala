package uk.ac.warwick.tabula.data.model.permissions

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import org.hibernate.annotations.Type
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.BuiltInRoleDefinition
import uk.ac.warwick.tabula.roles.RoleDefinition
import scala.collection.immutable.ListMap
import uk.ac.warwick.tabula.data.model.HibernateVersioned

@Entity
class CustomRoleDefinition extends RoleDefinition with HibernateVersioned with GeneratedId with PermissionsTarget {

	// The department which owns this definition - probably want to expand this to include sub-departments later
	@ManyToOne
	@JoinColumn(name = "department_id")
	@BeanProperty var department: Department = _
	
	@BeanProperty var name: String = _
	
	// The role definition that this role infers from; can be a built in role definition
	// or a custom role definition
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "custom_base_role_id")
	@BeanProperty var customBaseRoleDefinition: CustomRoleDefinition = _
	
	@OneToMany(mappedBy="customBaseRoleDefinition", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var subDefinitions:JList[CustomRoleDefinition] = List()
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.permissions.BuiltInRoleDefinitionUserType")
	@BeanProperty var builtInBaseRoleDefinition: BuiltInRoleDefinition = _
	
	def baseRoleDefinition: RoleDefinition = Option(customBaseRoleDefinition) getOrElse builtInBaseRoleDefinition
	def baseRoleDefinition_(definition: RoleDefinition) = definition match {
		case customDefinition: CustomRoleDefinition => {
			customBaseRoleDefinition = customDefinition
			builtInBaseRoleDefinition = null
		}
		case builtInDefinition: BuiltInRoleDefinition => {
			customBaseRoleDefinition = null
			builtInBaseRoleDefinition = builtInDefinition
		}
		case _ => {
			customBaseRoleDefinition = null
			builtInBaseRoleDefinition = null
		}
	}
	
	// A set of role overrides
	@OneToMany(mappedBy="customRoleDefinition", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var overrides:JList[RoleOverride] = List()
	
	def permissionsParents = 
		Seq(Option(department)).flatten
		
	def permissions(scope: Option[PermissionsTarget]) = {
		val basePermissions = baseRoleDefinition.permissions(scope)
		
		val (additionOverrides, removalOverrides) = overrides.partition(_.overrideType)
		val additions = additionOverrides.map { _.permission -> scope }
		val removals = removalOverrides.map { _.permission }
		
		(basePermissions ++ additions) -- removals
	}
		
	def subRoles(scope: Option[PermissionsTarget]) = baseRoleDefinition.subRoles(scope)

}