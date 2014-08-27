package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import org.hibernate.annotations._
import org.joda.time.DateTime
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{FormField, SavedFormValue}
import org.hibernate.annotations.AccessType
import javax.persistence.Entity
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService

@Entity @AccessType("field")
class MarkerFeedback extends GeneratedId with FeedbackAttachments with ToEntityReference with CanBeDeleted {
	type Entity = MarkerFeedback

	def this(parent:Feedback){
		this()
		feedback = parent
	}

	@transient
	var userLookup = Wire[UserLookupService]("userLookup")

	def getFeedbackPosition: FeedbackPosition = feedback.getFeedbackPosition(this)

	def getMarkerUsercode: Option[String] = {
		val student = feedback.universityId
		val assignment = feedback.assignment
		val workflow = assignment.markingWorkflow
		getFeedbackPosition match {
			case FirstFeedback => workflow.getStudentsFirstMarker(assignment, student)
			case SecondFeedback => workflow.getStudentsSecondMarker(assignment, student)
			case ThirdFeedback => workflow.getStudentsFirstMarker(assignment, student)
			case _ => None
		}
	}

	def getMarkerUser: User = userLookup.getUserByUserId(getMarkerUsercode.get)

	@OneToOne(fetch = FetchType.LAZY, optional = false, cascade=Array())
	@JoinColumn(name = "feedback_id", nullable = false)
	@ForeignKey(name = "none")
	var feedback: Feedback = _

	@Column(name = "uploaded_date")
	var uploadedDate: DateTime = new DateTime

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var mark: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var grade: Option[String] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.MarkingStateUserType")
	var state : MarkingState = _

	var rejectionComments: String = _

	@OneToMany(mappedBy = "markerFeedback", fetch = FetchType.LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	@Fetch(FetchMode.JOIN)
	var attachments: JList[FileAttachment] = JArrayList()

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.markerFeedback = this
		attachments.add(attachment)
	}

	@OneToMany(mappedBy = "markerFeedback", cascade = Array(ALL))
	var customFormValues: JSet[SavedFormValue] = JHashSet()

	def getValue(field: FormField): Option[SavedFormValue] = {
		customFormValues.find( _.name == field.name )
	}

	def hasContent = hasMarkOrGrade || hasFeedback

	def hasMarkOrGrade = hasMark || hasGrade

	def hasMark: Boolean = mark.isDefined

	def hasGrade: Boolean = grade.isDefined

	def hasFeedback = attachments != null && attachments.size() > 0

	def hasComments = customFormValues.exists(_.value != null)

	override def toEntityReference = new MarkerFeedbackEntityReference().put(this)
}
