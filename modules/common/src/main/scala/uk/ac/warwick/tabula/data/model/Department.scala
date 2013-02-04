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
class Department extends GeneratedId with PostLoadBehaviour with PermissionsTarget {
  
	@BeanProperty var code:String = null
	
	@BeanProperty var name:String = null
	
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var modules:JList[Module] = List()
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="ownersgroup_id")
	@BeanProperty var owners:UserGroup = new UserGroup
	
	@BeanProperty var collectFeedbackRatings:Boolean = false

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()
	
	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var markSchemes:JList[MarkScheme] = ArrayList()

	// settings for extension requests
	@BeanProperty var allowExtensionRequests:JBoolean = false
	@BeanProperty var extensionGuidelineSummary:String = null
	@BeanProperty var extensionGuidelineLink:String = null
	
	/** The group of extension managers */
	@OneToOne(cascade = Array(CascadeType.ALL))
	@JoinColumn(name = "extension_managers_id")
	@BeanProperty var extensionManagers = new UserGroup()

	def formattedGuidelineSummary:String = Option(extensionGuidelineSummary) map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map{ p => <p>{p}</p> }
		(NodeSeq fromSeq nodes).toString()
	} getOrElse("")

	def isOwnedBy(userId:String) = owners.includes(userId)
	def addOwner(owner:String) = ensureOwners.addUser(owner)
	def removeOwner(owner:String) = ensureOwners.removeUser(owner)

	def canRequestExtension = allowExtensionRequests != null && allowExtensionRequests
	def isExtensionManager(user:String) = extensionManagers!=null && extensionManagers.includes(user)

	def addFeedbackForm(form:FeedbackTemplate) = feedbackTemplates.add(form)

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureOwners
	}

	def ensureOwners = {
		if (owners == null) owners = new UserGroup
		owners
	}
	
	def permissionsParents = Seq()

	override def toString = "Department(" + code + ")"

	@BeanProperty var showStudentName:JBoolean = false
	
}
