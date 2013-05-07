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

	def collectFeedbackRatings = getBooleanSetting(Settings.CollectFeedbackRatings) getOrElse(false)
	def collectFeedbackRatings_= (collect: Boolean) = settings += (Settings.CollectFeedbackRatings -> collect)

	// settings for extension requests
	def allowExtensionRequests = getBooleanSetting(Settings.AllowExtensionRequests) getOrElse(false)
	def allowExtensionRequests_= (allow: Boolean) = settings += (Settings.AllowExtensionRequests -> allow)

	def extensionGuidelineSummary = getStringSetting(Settings.ExtensionGuidelineSummary).orNull
	def extensionGuidelineSummary_= (summary: String) = settings += (Settings.ExtensionGuidelineSummary -> summary)

	def extensionGuidelineLink = getStringSetting(Settings.ExtensionGuidelineLink).orNull
	def extensionGuidelineLink_= (link: String) = settings += (Settings.ExtensionGuidelineLink -> link)

	def showStudentName = getBooleanSetting(Settings.ShowStudentName) getOrElse(false)
	def showStudentName_= (showName: Boolean) = settings += (Settings.ShowStudentName -> showName)

	def plagiarismDetectionEnabled = getBooleanSetting(Settings.PlagiarismDetection, true)
	def plagiarismDetectionEnabled_= (enabled: Boolean) = settings += (Settings.PlagiarismDetection -> enabled)

	def assignmentInfoView = getStringSetting(Settings.AssignmentInfoView) getOrElse(Assignment.Settings.InfoViewType.Default)
	def assignmentInfoView_= (setting: String) = settings += (Settings.AssignmentInfoView -> setting)

	def personalTutorSource = getStringSetting(Settings.PersonalTutorSource) getOrElse(Department.Settings.PersonalTutorSourceDefault)
	def personalTutorSource_= (ptSource: String) = settings += (Settings.PersonalTutorSource -> ptSource)

	// FIXME belongs in Freemarker
	def formattedGuidelineSummary:String = Option(extensionGuidelineSummary) map { raw =>
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
		personalTutorSource == null || personalTutorSource == "local"
	}

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureSettings
	}

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	var grantedRoles:JList[DepartmentGrantedRole] = JArrayList()

	/**
	  * Although a department may have a parent, we don't actually
	  * want to inherit permissions from it. We can add users explicitly
	  * to the child department if they need access there.
	  *
	  * This is open to discussion and change.
	  */
	def permissionsParents = Option(parent).toSeq

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

		val PersonalTutorSourceDefault = "local"
	}
}
