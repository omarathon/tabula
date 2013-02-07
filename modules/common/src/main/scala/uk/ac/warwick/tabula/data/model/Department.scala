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

@Entity @AccessType("field")
class Department extends GeneratedId with PostLoadBehaviour with SettingsMap[Department] with PermissionsTarget {
	import Department._
  
	@BeanProperty var code:String = null
	
	@BeanProperty var name:String = null
	
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var modules:JList[Module] = List()
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="ownersgroup_id")
	@BeanProperty var owners:UserGroup = new UserGroup

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()
	
	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var markSchemes:JList[MarkScheme] = ArrayList()
	
	/* Legacy Properties. Remove these once the settings map is completely in use */
	@BeanProperty @Column(name="collectFeedbackRatings") var collectFeedbackRatingsLegacy:Boolean = false
	def isCollectFeedbackRatings = getBooleanSetting(Settings.CollectFeedbackRatings, collectFeedbackRatingsLegacy)

	// settings for extension requests
	@BeanProperty @Column(name="allowExtensionRequests") var allowExtensionRequestsLegacy:JBoolean = false
	def isAllowExtensionRequests = getBooleanSetting(Settings.AllowExtensionRequests, if (allowExtensionRequestsLegacy != null) allowExtensionRequestsLegacy else false)
	
	@BeanProperty @Column(name="extensionGuidelineSummary") var extensionGuidelineSummaryLegacy:String = null
	def getExtensionGuidelineSummary = getStringSetting(Settings.ExtensionGuidelineSummary, extensionGuidelineSummaryLegacy)
	
	@BeanProperty @Column(name="extensionGuidelineLink") var extensionGuidelineLinkLegacy:String = null
	def getExtensionGuidelineLink = getStringSetting(Settings.ExtensionGuidelineLink, extensionGuidelineLinkLegacy)
	
	/** The group of extension managers */
	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "extension_managers_id")
	@BeanProperty var extensionManagers = new UserGroup()

	def formattedGuidelineSummary:String = Option(getExtensionGuidelineSummary) map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map{ p => <p>{p}</p> }
		(NodeSeq fromSeq nodes).toString()
	} getOrElse("")

	def isOwnedBy(userId:String) = owners.includes(userId)
	def addOwner(owner:String) = ensureOwners.addUser(owner)
	def removeOwner(owner:String) = ensureOwners.removeUser(owner)

	def canRequestExtension = isAllowExtensionRequests
	def isExtensionManager(user:String) = extensionManagers!=null && extensionManagers.includes(user)

	def addFeedbackForm(form:FeedbackTemplate) = feedbackTemplates.add(form)

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureOwners
		ensureSettings
	}

	def ensureOwners = {
		if (owners == null) owners = new UserGroup
		owners
	}
	
	def permissionsParents = Seq()

	override def toString = "Department(" + code + ")"

	@BeanProperty var showStudentName:JBoolean = false
	
}

object Department {
	object Settings {
		val CollectFeedbackRatings = "collectFeedbackRatings"
			
		val AllowExtensionRequests = "allowExtensionRequests"
		val ExtensionGuidelineSummary = "extensionGuidelineSummary"
		val ExtensionGuidelineLink = "extensionGuidelineLink"
	}
}
