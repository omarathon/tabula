package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions.seqAsJavaList
import scala.reflect.BeanProperty
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

@Entity @AccessType("field")
class Department extends GeneratedId with PostLoadBehaviour with SettingsMap[Department] with PermissionsTarget {
	import Department._
  
	@BeanProperty var code:String = null
	
	@BeanProperty var name:String = null
	
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var modules:JList[Module] = List()

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()
	
	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var markingWorkflows:JList[MarkingWorkflow] = ArrayList()
	
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var customRoleDefinitions:JList[CustomRoleDefinition] = List()
	
	def isCollectFeedbackRatings = collectFeedbackRatings
	def collectFeedbackRatings = getBooleanSetting(Settings.CollectFeedbackRatings) getOrElse(false)
	def collectFeedbackRatings_= (collect: Boolean) = settings += (Settings.CollectFeedbackRatings -> collect)

	// settings for extension requests
	def isAllowExtensionRequests = allowExtensionRequests
	def allowExtensionRequests = getBooleanSetting(Settings.AllowExtensionRequests) getOrElse(false)
	def allowExtensionRequests_= (allow: Boolean) = settings += (Settings.AllowExtensionRequests -> allow)
	
	def getExtensionGuidelineSummary = extensionGuidelineSummary
	def extensionGuidelineSummary = getStringSetting(Settings.ExtensionGuidelineSummary) orNull
	def extensionGuidelineSummary_= (summary: String) = settings += (Settings.ExtensionGuidelineSummary -> summary)
	
	def getExtensionGuidelineLink = extensionGuidelineLink
	def extensionGuidelineLink = getStringSetting(Settings.ExtensionGuidelineLink) orNull
	def extensionGuidelineLink_= (link: String) = settings += (Settings.ExtensionGuidelineLink -> link)
	
	def isShowStudentName = showStudentName
	def showStudentName = getBooleanSetting(Settings.ShowStudentName) getOrElse(false)
	def showStudentName_= (showName: Boolean) = settings += (Settings.ShowStudentName -> showName)

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
	
	def isPlagiarismDetectionEnabled = getBooleanSetting(Settings.PlagiarismDetection, true)

	def addFeedbackForm(form:FeedbackTemplate) = feedbackTemplates.add(form)

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureSettings
	}
	
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
