package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.model.forms.{FormField, SavedFormValue}
import java.util.HashSet


@Entity @AccessType("field")
class Feedback extends GeneratedId with PermissionsTarget {

	def this(universityId: String) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(fetch = FetchType.LAZY, cascade=Array(PERSIST, MERGE), optional = false)
	var assignment: Assignment = _

	def permissionsParents = Option(assignment).toStream

	var uploaderId: String = _

	@Column(name = "uploaded_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var uploadedDate: DateTime = new DateTime

	var universityId: String = _

	var released: JBoolean = false

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
	var ratingPrompt: Option[Boolean] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBooleanUserType")
	var ratingHelpful: Option[Boolean] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var actualMark: Option[Int] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var agreedMark: Option[Int] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var actualGrade: Option[String] = None
	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var agreedGrade: Option[String] = None

	@OneToOne(cascade=Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "first_marker_feedback")
	var firstMarkerFeedback: MarkerFeedback = _

	@OneToOne(cascade=Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "second_marker_feedback")
	var secondMarkerFeedback: MarkerFeedback = _

	@Column(name = "released_date")
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentDateTime")
	var releasedDate: DateTime = _

	@OneToMany(mappedBy = "feedback", cascade = Array(ALL))
	var customFormValues: JSet[SavedFormValue] = new HashSet

	def getValue(field: FormField): Option[SavedFormValue] = {
		customFormValues.find( _.name == field.name )
	}

	def onlineFeedbackComments: Option[SavedFormValue] = assignment.feedbackCommentsField.flatMap(getValue(_))

	// Getters for marker feedback either return the marker feedback or create a new empty one if none exist
	def retrieveFirstMarkerFeedback:MarkerFeedback = {
		Option(firstMarkerFeedback).getOrElse({
			firstMarkerFeedback = new MarkerFeedback(this)
			firstMarkerFeedback
		})
	}

	def retrieveSecondMarkerFeedback:MarkerFeedback = {
		Option(secondMarkerFeedback).getOrElse({
			secondMarkerFeedback = new MarkerFeedback(this)
			secondMarkerFeedback
		})
	}

	// if the feedback has no marks or attachments then it is a placeholder for marker feedback
	def isPlaceholder = !(hasMarkOrGrade || hasAttachments)

	def hasMarkOrGrade = hasMark || hasGrade

	def hasMark: Boolean = actualMark match {
		case Some(int) => true
		case None => false
	}

	def hasGrade: Boolean = actualGrade match {
		case Some(string) => true
		case None => false
	}

	def hasAttachments: Boolean = !attachments.isEmpty

	def hasOnlineFeedback: Boolean = onlineFeedbackComments.isDefined

	/**
	 * Returns the released flag of this feedback,
	 * OR false if unset.
	 */
	def checkedReleased: Boolean = Option(released) match {
		case Some(bool) => bool
		case None => false
	}

	@OneToMany(mappedBy = "feedback", fetch = FetchType.LAZY, cascade=Array(ALL))
	var attachments: JList[FileAttachment] = JArrayList()

	def mostRecentAttachmentUpload =
		if (attachments.isEmpty) null
		else attachments.maxBy { _.dateUploaded }.dateUploaded

	/* Adds new attachments to the feedback. Ignores feedback that has already been uploaded and overwrites attachments
	   with the same name as exiting attachments. Returns the attachments that wern't ignored. */
	def addAttachments(fileAttachments: Seq[FileAttachment]) : Seq[FileAttachment] = fileAttachments.filter { a =>
		val isIdentical = attachments.exists(f => f.name == a.name && f.isDataEqual(a))
		if (!isIdentical) {
			// if an attachment with the same name as this one exists then replace it
			val duplicateAttachment = attachments.find(_.name == a.name)
			duplicateAttachment.foreach(removeAttachment(_))
			addAttachment(a)
		}
		!isIdentical
	}



	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedback = this
		attachments.add(attachment)
	}

	def removeAttachment(attachment: FileAttachment) = {
		attachment.feedback = null
		attachments.remove(attachment)
	}

	def clearAttachments() {
		for(attachment <- attachments){
			attachment.feedback = null
		}
		attachments.clear()
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
}
