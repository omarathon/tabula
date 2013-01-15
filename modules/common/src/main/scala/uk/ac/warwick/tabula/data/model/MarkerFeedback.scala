package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Fetch, FetchMode, Type, AccessType}
import uk.ac.warwick.tabula.actions.{Deleteable, Viewable}
import reflect.BeanProperty
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.ArrayList

@Entity @AccessType("field")
class MarkerFeedback extends GeneratedId with Viewable with Deleteable {

	def this(parent:Feedback){
		this()
		feedback = parent
	}

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "feedback_id")
	@BeanProperty var feedback: Feedback = _

	@Column(name = "uploaded_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate: DateTime = new DateTime

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var mark: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.MarkingStateUserType")
	@BeanProperty var state : MarkingState = _

	@OneToMany(mappedBy = "markerFeedback", fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	var attachments: JList[FileAttachment] = ArrayList()

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.markerFeedback = this
		attachments.add(attachment)
	}

	def hasMarks = mark.isDefined
	def hasFeedback = attachments != null && attachments.size() > 0
}
