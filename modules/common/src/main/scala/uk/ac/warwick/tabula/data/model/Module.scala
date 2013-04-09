package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.util.matching.Regex
import org.hibernate.annotations.AccessType
import javax.persistence._
import javax.validation.constraints._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.data.model.permissions.ModuleGrantedRole
import org.hibernate.annotations.ForeignKey
import uk.ac.warwick.tabula.roles.ModuleAssistantRoleDefinition

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "module.code", query = "select m from Module m where code = :code"),
	new NamedQuery(name = "module.department", query = "select m from Module m where department = :department")))
class Module extends GeneratedId with PermissionsTarget {

	def this(code: String = null, department: Department = null) {
		this()
		this.code = code
		this.department = department
	}

	var code: String = _
	var name: String = _

	// The managers are markers/moderators who upload feedback. 
	// They can also publish feedback.
	// Module assistants can't publish feedback
	@transient 
	var permissionsService = Wire.auto[PermissionsService]
	@transient 
	lazy val managers = permissionsService.ensureUserGroupFor(this, ModuleManagerRoleDefinition)
	@transient 
	lazy val assistants = permissionsService.ensureUserGroupFor(this, ModuleAssistantRoleDefinition)

	@ManyToOne
	@JoinColumn(name = "department_id")
	var department: Department = _
	
	def permissionsParents = Option(department).toSeq
	
	@OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var assignments: java.util.List[Assignment] = ArrayList()

	var active: Boolean = _
	
	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	var grantedRoles:JList[ModuleGrantedRole] = ArrayList()

	override def toString = "Module[" + code + "]"
}

object Module {

	// <modulecode> "-" <cats>
	// where cats can be a decimal number.
	private val ModuleCatsPattern = new Regex("""(.+?)-(\d+(?:\.\d+)?)""")

	def nameFromWebgroupName(groupName: String): String = groupName.indexOf("-") match {
		case -1 => groupName
		case i: Int => groupName.substring(i + 1)
	}

	def stripCats(fullModuleName: String): String = fullModuleName match {
		case ModuleCatsPattern(module, cats) => module
		case _ => throw new IllegalArgumentException(fullModuleName + " didn't match pattern")
	}

	def extractCats(fullModuleName: String): Option[String] = fullModuleName match {
		case ModuleCatsPattern(module, cats) => Some(cats)
		case _ => None
	}
}