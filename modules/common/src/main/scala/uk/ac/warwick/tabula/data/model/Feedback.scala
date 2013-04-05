package uk.ac.warwick.tabula.data.model

import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence._
import uk.ac.warwick.tabula.JavaImports.JBoolean
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import javax.persistence.CascadeType


@Entity @AccessType("field")
class Feedback extends GeneratedId with PermissionsTarget {

	def this(universityId: String) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	var assignment: Assignment = _
	
	def permissionsParents = Seq(Option(assignment)).flatten

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

	@OneToOne(cascade=Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "first_marker_feedback")
	var firstMarkerFeedback: MarkerFeedback = _

	@OneToOne(cascade=Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "second_marker_feedback")
	var secondMarkerFeedback: MarkerFeedback = _

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
	
	def mostRecentAttachmentUpload = attachments.maxBy {
		_.dateUploaded
	}.dateUploaded

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.feedback = this
		attachments.add(attachment)
	}

	def clearAttachments() {
		for(attachment <- attachments){
			attachment.feedback = null
		}
		attachments = ArrayList()
	}

	/**
	 * Whether ratings are being collected for this feedback.
	 * Doesn't take into account whether the ratings feature is enabled, so you
	 * need to check that separately.
	 */
	def collectRatings: Boolean = assignment.module.department.isCollectFeedbackRatings

	/**
	 * Whether marks are being collected for this feedback.
	 * Doesn't take into account whether the marks feature is enabled, so you
	 * need to check that separately.
	 */
	def collectMarks: Boolean = assignment.collectMarks
}
