package uk.ac.warwick.courses.data.model
import scala.collection.JavaConversions.seqAsJavaList
import scala.reflect.BeanProperty
import org.hibernate.annotations.AccessType
import javax.persistence._
import uk.ac.warwick.courses.data._
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.JavaImports._
import xml.NodeSeq
import scala.Array
import uk.ac.warwick.courses.helpers.ArrayList

@Entity @AccessType("field")
class Department extends GeneratedId with PostLoadBehaviour with Viewable with Manageable {
  
	@BeanProperty var code:String = null
	
	@BeanProperty var name:String = null
	
	@OneToMany(mappedBy="department", fetch = FetchType.LAZY)
	@BeanProperty var modules:JList[Module] = List()
	
	@OneToOne(cascade=Array(CascadeType.ALL))
	@JoinColumn(name="ownersgroup_id")
	@BeanProperty var owners:UserGroup = new UserGroup
	
	@BeanProperty var collectFeedbackRatings:Boolean = true

	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
	@BeanProperty var feedbackTemplates:JList[FeedbackTemplate] = ArrayList()
	
	@OneToMany(mappedBy = "department", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BeanProperty var markSchemes:JList[MarkScheme] = ArrayList()

	// settings for extension requests
	@BeanProperty var allowExtensionRequests:JBoolean = false
	@BeanProperty var extensionGuidelineSummary:String = null
	@BeanProperty var extensionGuidelineLink:String = null

	@OneToMany(mappedBy = "department")
	@BeanProperty var markSchemes: JList[MarkScheme] = ArrayList()

	def formattedGuidelineSummary:String = Option(extensionGuidelineSummary) map { raw =>
		val Splitter = """\s*\n(\s*\n)+\s*""".r // two+ newlines, with whitespace
		val nodes = Splitter.split(raw).map{ p => <p>{p}</p> }
		(NodeSeq fromSeq nodes).toString()
	} getOrElse("")

	def isOwnedBy(userId:String) = owners.includes(userId)
	def addOwner(owner:String) = ensureOwners.addUser(owner)
	def removeOwner(owner:String) = ensureOwners.removeUser(owner)

	def canRequestExtension = allowExtensionRequests != null && allowExtensionRequests

	def addFeedbackForm(form:FeedbackTemplate) = feedbackTemplates.add(form)

	// If hibernate sets owners to null, make a new empty usergroup
	override def postLoad {
		ensureOwners
	}

	def ensureOwners = {
		if (owners == null) owners = new UserGroup
		owners
	}

	override def toString = "Department(" + code + ")"

}