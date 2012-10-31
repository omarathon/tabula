package uk.ac.warwick.courses.data.model

import reflect.BeanProperty
import javax.persistence.{Entity, ManyToOne, JoinColumn, OneToOne}
import org.hibernate.annotations.AccessType

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

	def attachFile(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedbackForm = this
		this.attachment = attachment
	}

}