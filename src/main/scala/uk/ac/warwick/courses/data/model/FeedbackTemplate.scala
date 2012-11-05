package uk.ac.warwick.courses.data.model

import reflect.BeanProperty
import javax.persistence._
import org.hibernate.annotations.AccessType
import scala.Array
import uk.ac.warwick.courses.JavaImports.JList
import uk.ac.warwick.courses.helpers.ArrayList

@Entity @AccessType("field")
class FeedbackTemplate extends GeneratedId {

	@BeanProperty var name:String = _
	@BeanProperty var description:String = _

	@OneToOne
	@JoinColumn(name="ATTACHMENT_ID")
	@BeanProperty var attachment: FileAttachment = _

	@ManyToOne
	@JoinColumn(name="DEPARTMENT_ID")
	@BeanProperty var department:Department = _

	@OneToMany(mappedBy = "feedbackTemplate", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL))
	@BeanProperty var assignments: JList[Assignment] = ArrayList()

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