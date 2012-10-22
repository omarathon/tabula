package uk.ac.warwick.courses.data.model.forms

import scala.collection.JavaConversions._
import org.hibernate.annotations.{Type, AccessType}
import uk.ac.warwick.courses.data.model.{FileAttachment, Assignment, GeneratedId}
import uk.ac.warwick.courses.actions.Deleteable
import scala.Array
import javax.persistence._
import javax.persistence.CascadeType._
import javax.validation.constraints.NotNull
import reflect.BeanProperty
import org.joda.time.DateTime
import javax.persistence.FetchType._
import uk.ac.warwick.courses.JavaImports._

@Entity @AccessType("field")
class Extension extends GeneratedId with Deleteable {

	def this(universityId:String=null) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="assignment_id")
	@BeanProperty var assignment:Assignment = _

	@NotNull
	@BeanProperty var userId:String =_

	@NotNull
	@BeanProperty var universityId:String =_

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var requestedExpiryDate:DateTime =_

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var expiryDate:DateTime =_

	@BeanProperty var reason:String =_

	@OneToMany(mappedBy="extension", fetch=LAZY)
	@BeanProperty var attachments:JSet[FileAttachment] = JSet()

	def nonEmptyAttachments = attachments.toSeq filter(_.hasData)

	def addAttachment(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.extension = this
		attachments.add(attachment)
	}

	@BeanProperty var approved:Boolean = false
	@BeanProperty var rejected:Boolean = false

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var requestedOn:DateTime =_

	@Type(`type`="org.joda.time.contrib.hibernate.PersistentDateTime")
	@BeanProperty var approvedOn:DateTime =_

	@BeanProperty var approvalComments:String =_

	// this was not requested by a student. i.e. was manually created by an administrator
	def isManual = requestedOn == null
	def isAwaitingApproval = !isManual && !approved && !rejected

}
