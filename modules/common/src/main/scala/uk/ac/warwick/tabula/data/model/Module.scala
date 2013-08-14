package uk.ac.warwick.tabula.data.model

import scala.util.matching.Regex
import org.hibernate.annotations.AccessType
import javax.persistence._
import javax.validation.constraints._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.permissions.ModuleGrantedRole
import org.hibernate.annotations.ForeignKey
import uk.ac.warwick.tabula.roles.ModuleAssistantRoleDefinition
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import scala.collection.JavaConverters._

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
	
	def permissionsParents = Option(department).toStream
	
	@OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var assignments: JList[Assignment] = JArrayList()

	def hasLiveAssignments = Option(assignments) match {
		case Some(a) => a.asScala.exists(_.isAlive)
		case None => false
	}

	
	@OneToMany(mappedBy = "module", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var groupSets: JList[SmallGroupSet] = JArrayList()

	var active: Boolean = _
	
	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	var grantedRoles:JList[ModuleGrantedRole] = JArrayList()

	override def toString = "Module[" + code + "]"


  // true if at least one of this module's SmallGroupSets has not been released to //both students and staff.
  def hasUnreleasedGroupSets():Boolean = {
    val allGroupSets = groupSets.asScala
    allGroupSets.exists(!_.fullyReleased)
  }
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
	
	// For sorting a collection by module code. Either pass to the sort function,
	// or expose as an implicit val.
	val CodeOrdering = Ordering.by[Module, String] ( _.code )
	val NameOrdering = Ordering.by[Module, String] ( _.name )
	
	// Companion object is one of the places searched for an implicit Ordering, so
	// this will be the default when ordering a list of modules.
	implicit val defaultOrdering = CodeOrdering

}