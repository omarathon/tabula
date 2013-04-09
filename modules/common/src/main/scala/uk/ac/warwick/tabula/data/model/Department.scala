package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import scala.xml.NodeSeq
import org.hibernate.annotations.AccessType
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.helpers.ArrayList
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

@Entity @AccessType("field")
class Department extends GeneratedId with PostLoadBehaviour with SettingsMap[Department] with PermissionsTarget {
	import Department._
  
	var code:String = null
	
	var name:String = null
	
	@OneToMany(mappedBy="parent", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var children:JList[Department] = ArrayList();
	
	@ManyToOne(fetch = FetchType.LAZY, optional=true)
	var parent:Department = null;
	
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var modules:JList[Module] = ArrayList()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var markingWorkflows:JList[MarkingWorkflow] = ArrayList()

	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	var customRoleDefinitions:JList[CustomRoleDefinition] = ArrayList()

	def isCollectFeedbackRatings = collectFeedbackRatings
	def collectFeedbackRatings = getBooleanSetting(Settings.CollectFeedbackRatings) getOrElse(false)
	def collectFeedbackRatings_= (collect: Boolean) = settings += (Settings.CollectFeedbackRatings -> collect)

	// settings for extension requests
	def isAllowExtensionRequests = allowExtensionRequests
	def allowExtensionRequests = getBooleanSetting(Settings.AllowExtensionRequests) getOrElse(false)
	def allowExtensionRequests_= (allow: Boolean) = settings += (Settings.AllowExtensionRequests -> allow)

	def getExtensionGuidelineSummary = extensionGuidelineSummary
	def extensionGuidelineSummary = getStringSetting(Settings.ExtensionGuidelineSummary).orNull
	def extensionGuidelineSummary_= (summary: String) = settings += (Settings.ExtensionGuidelineSummary -> summary)

	def getExtensionGuidelineLink = extensionGuidelineLink
	def extensionGuidelineLink = getStringSetting(Settings.ExtensionGuidelineLink).orNull
	def extensionGuidelineLink_= (link: String) = settings += (Settings.ExtensionGuidelineLink -> link)

	def isShowStudentName = showStudentName
	def showStudentName = getBooleanSetting(Settings.ShowStudentName) getOrElse(false)
	def showStudentName_= (showName: Boolean) = settings += (Settings.ShowStudentName -> showName)

	def isPlagiarismDetectionEnabled = plagiarismDetectionEnabled
	def plagiarismDetectionEnabled = getBooleanSetting(Settings.PlagiarismDetection, true)
	def plagiarismDetectionEnabled_= (enabled: Boolean) = settings += (Settings.PlagiarismDetection -> enabled)

	def formattedGuidelineSummary:String = Option(getExtensionGuidelineSummary) map { raw =>
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
	def addOwner(owner:String) = owners.addUser(owner)
	def removeOwner(owner:String) = owners.removeUser(owner)

	def canRequestExtension = isAllowExtensionRequests
	def isExtensionManager(user:String) = extensionManagers!=null && extensionManagers.includes(user)

	def addFeedbackForm(form:FeedbackTemplate) = feedbackTemplates.add(form)

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureSettings
	}

	@OneToMany(mappedBy="scope", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@ForeignKey(name="none")
	@BeanProperty var grantedRoles:JList[DepartmentGrantedRole] = ArrayList()
	
	def permissionsParents = Seq()

	override def toString = "Department(" + code + ")"

}

object Department {
	object Settings {
		val CollectFeedbackRatings = "collectFeedbackRatings"

		val AllowExtensionRequests = "allowExtensionRequests"
		val ExtensionGuidelineSummary = "extensionGuidelineSummary"
		val ExtensionGuidelineLink = "extensionGuidelineLink"

		val ShowStudentName = "showStudentName"

		val PlagiarismDetection = "plagiarismDetection"
	}
}
