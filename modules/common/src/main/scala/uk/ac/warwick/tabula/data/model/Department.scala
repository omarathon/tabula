package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.xml.NodeSeq
import org.hibernate.annotations.AccessType
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition
import org.hibernate.annotations.JoinColumnsOrFormulas
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinFormula
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedRole
import org.hibernate.annotations.ForeignKey
import scala.annotation.tailrec
import uk.ac.warwick.tabula.data.model.groups.WeekRange

@Entity @AccessType("field")
class Department extends GeneratedId with PostLoadBehaviour with SettingsMap[Department] with PermissionsTarget {
	import Department._

	var code:String = null

	var name:String = null

	@OneToMany(mappedBy="parent", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var children:JList[Department] = JArrayList();

	@ManyToOne(fetch = FetchType.LAZY, optional=true)
	var parent:Department = null;

	// No orphanRemoval as it makes it difficult to move modules between Departments.
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = false)
	var modules:JList[Module] = JArrayList()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var feedbackTemplates:JList[FeedbackTemplate] = JArrayList()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var markingWorkflows:JList[MarkingWorkflow] = JArrayList()

	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var customRoleDefinitions:JList[CustomRoleDefinition] = JArrayList()

	val collectFeedbackRatings = Setting[Boolean](key=Settings.CollectFeedbackRatings, default=false)

	// settings for extension requests
	val allowExtensionRequests = Setting[Boolean](key=Settings.AllowExtensionRequests, default=false)
	val extensionGuidelineSummary = OptionSetting[String](key=Settings.ExtensionGuidelineSummary)
	val extensionGuidelineLink = OptionSetting[String](key=Settings.ExtensionGuidelineLink)
	val showStudentName = Setting[Boolean](key=Settings.ShowStudentName, default=false)
	val plagiarismDetectionEnabled = Setting[Boolean](key=Settings.PlagiarismDetection, default=true)
	val assignmentInfoView = Setting[String](key=Settings.AssignmentInfoView, default=Assignment.Settings.InfoViewType.Default)
	val personalTutorSource = Setting[String](key=Settings.PersonalTutorSource, default=Department.Settings.PersonalTutorSourceValues.Local)
	val weekNumberingSystem = Setting[String](key=Settings.WeekNumberingSystem, default=WeekRange.NumberingSystem.Default)

	// FIXME belongs in Freemarker
	def formattedGuidelineSummary:String = extensionGuidelineSummary.value map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map{ p => <p>{p}</p> }
		(NodeSeq fromSeq nodes).toString()
	} getOrElse("")

	@transient
	var permissionsService = Wire.auto[PermissionsService]
	@transient
	lazy val owners = permissionsService.ensureUserGroupFor(this, DepartmentalAdministratorRoleDefinition)
	@transient
	lazy val extensionManagers = permissionsService.ensureUserGroupFor(this, ExtensionManagerRoleDefinition)

	def isOwnedBy(userId:String) = owners.includes(userId)

	@deprecated("Use ModuleAndDepartmentService.addOwner", "35")
	def addOwner(owner:String) = owners.addUser(owner)

	@deprecated("Use ModuleAndDepartmentService.removeOwner", "35")
	def removeOwner(owner:String) = owners.removeUser(owner)

	def canRequestExtension = allowExtensionRequests
	def isExtensionManager(user:String) = extensionManagers!=null && extensionManagers.includes(user)

	def addFeedbackForm(form:FeedbackTemplate) = feedbackTemplates.add(form)

	def canEditPersonalTutors: Boolean = {
		personalTutorSource.value == Settings.PersonalTutorSourceValues.Local
	}

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureSettings
	}

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	var grantedRoles:JList[DepartmentGrantedRole] = JArrayList()

	def permissionsParents = Option(parent).toStream
	
	/** The 'top' ancestor of this department, or itself if
	  * it has no parent.
	  */
	@tailrec
	final def rootDepartment: Department =
		if (parent == null) this
		else parent.rootDepartment

	def hasParent = (parent != null)

	def isUpstream = !hasParent

	override def toString = "Department(" + code + ")"

}

object Department {
	object Settings {
		val CollectFeedbackRatings = "collectFeedbackRatings"

		val AllowExtensionRequests = "allowExtensionRequests"
		val ExtensionGuidelineSummary = "extensionGuidelineSummary"
		val ExtensionGuidelineLink = "extensionGuidelineLink"

		val ShowStudentName = "showStudentName"
		val AssignmentInfoView = "assignmentInfoView"

		val PlagiarismDetection = "plagiarismDetection"

		val PersonalTutorSource = "personalTutorSource"
			
		val WeekNumberingSystem = "weekNumberSystem"

		object PersonalTutorSourceValues {
			val Local = "local"
			val Sits = "SITS"
		}
	}
}
