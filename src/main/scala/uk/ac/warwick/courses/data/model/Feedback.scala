package uk.ac.warwick.courses.data.model
import uk.ac.warwick.courses.JavaImports._
import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.joda.time.DateTime
import javax.persistence.Entity
import uk.ac.warwick.courses.helpers.ArrayList
import javax.persistence.Column
import org.hibernate.annotations.Type
import javax.persistence.ManyToOne
import scala.reflect.BeanProperty
import javax.persistence.FetchType
import javax.persistence.OneToMany
import javax.persistence.CascadeType
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.actions.Viewable
import uk.ac.warwick.courses.actions.Deleteable
import org.springframework.beans.factory.annotation.Autowired

@Entity @AccessType("field")
class Feedback extends GeneratedId with Viewable with Deleteable {

	def this(universityId: String) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@BeanProperty var assignment: Assignment = _

	var uploaderId: String = _

	@Column(name = "uploaded_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate: DateTime = new DateTime

	var universityId: String = _

	var released: JBoolean = false

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionBooleanUserType")
	var ratingPrompt: Option[Boolean] = None
	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionBooleanUserType")
	var ratingHelpful: Option[Boolean] = None

	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	var actualMark: Option[Int] = None
	@Type(`type` = "uk.ac.warwick.courses.data.model.OptionIntegerUserType")
	var agreedMark: Option[Int] = None
	var actualGrade: String = _
	var agreedGrade: String = _

	def hasMarkOrGrade = hasMark || hasGrade

	def hasMark: Boolean = actualMark match {
		case Some(int) => true
		case None => false
	}

	def hasGrade: Boolean = Option(actualGrade) match {
		case Some(string) => true
		case None => false
	}

	def hasAttachments: Boolean = !attachments.isEmpty

	/**
	 * Returns the released flag of this feedback,
	 * OR false if unset.
	 */
	def checkedReleased: Boolean = Option(released) match {
		case Some(bool) => bool
		case None => false
	}

	@OneToMany(mappedBy = "feedback", fetch = FetchType.LAZY)
	var attachments: JList[FileAttachment] = ArrayList()

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedback = this
		attachments.add(attachment)
	}

	/**
	 * Whether ratings are being collected for this feedback.
	 * Doesn't take into account whether the ratings feature is enabled, so you
	 * need to check that separately.
	 */
	def collectRatings: Boolean = assignment.module.department.collectFeedbackRatings

	/**
	 * Whether marks are being collected for this feedback.
	 * Doesn't take into account whether the marks feature is enabled, so you
	 * need to check that separately.
	 */
	def collectMarks: Boolean = assignment.collectMarks

    def getUploadedDateString = {
        val format = new java.text.SimpleDateFormat("dd-MM-yyyy")
        format.format(uploadedDate)
    }
}
