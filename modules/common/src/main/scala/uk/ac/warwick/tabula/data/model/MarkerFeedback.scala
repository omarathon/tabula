package uk.ac.warwick.tabula.data.model

import scala.beans.BeanProperty

import org.hibernate.annotations.{Fetch, FetchMode, Type, AccessType}
import org.joda.time.DateTime

import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.ArrayList

@Entity @AccessType("field")
class MarkerFeedback extends GeneratedId {

	def this(parent:Feedback){
		this()
		feedback = parent
	}

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade=Array())
	@JoinColumn(name = "feedback_id")
	var feedback: Feedback = _

	@Column(name = "uploaded_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate: DateTime = new DateTime

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var mark: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var grade: Option[String] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.MarkingStateUserType")
	var state : MarkingState = _

	@OneToMany(mappedBy = "markerFeedback", fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	var attachments: JList[FileAttachment] = ArrayList()

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.markerFeedback = this
		attachments.add(attachment)
	}

	def hasMarkOrGrade = hasMark || hasGrade

	def hasMark: Boolean = mark.isDefined

	def hasGrade: Boolean = grade.isDefined

	def hasFeedback = attachments != null && attachments.size() > 0
}
