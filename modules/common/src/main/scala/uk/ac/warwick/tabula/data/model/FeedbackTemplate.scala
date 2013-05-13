package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConverters._
import reflect.BeanProperty
import javax.persistence._
import javax.persistence.CascadeType._
import org.hibernate.annotations.AccessType
import scala.Array
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity @AccessType("field")
class FeedbackTemplate extends GeneratedId with PermissionsTarget {

	var name:String = _
	var description:String = _

	@OneToOne(orphanRemoval=true, cascade=Array(ALL))
	@JoinColumn(name="ATTACHMENT_ID")
	var attachment: FileAttachment = _

	@ManyToOne
	@JoinColumn(name="DEPARTMENT_ID")
	var department:Department = _

	@OneToMany(mappedBy = "feedbackTemplate", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	var assignments: JList[Assignment] = JArrayList()
	
	/* For permission parents, we include both the department and any assignments linked to this template */
	def permissionsParents = 
		Option[PermissionsTarget](department).toStream.append(Option(assignments) match {
			case Some(assignments) => assignments.asScala.toStream
			case _ => Stream.empty
		})

	def countLinkedAssignments = Option(assignments) match { case Some(a) => a.size()
		case None => 0
	}

	def hasAssignments = countLinkedAssignments > 0

	def attachFile(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedbackForm = this
		this.attachment = attachment
	}

}