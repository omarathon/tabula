package uk.ac.warwick.tabula.data.model

import java.util.HashSet

import scala.collection.JavaConversions._

import org.hibernate.annotations.{BatchSize, AccessType, Type}
import org.joda.time.DateTime

import javax.persistence._
import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{SavedFormValue, FormField}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User

@Entity @AccessType("field")
class Submission extends GeneratedId with PermissionsTarget {

	@transient
	var userLookup = Wire[UserLookupService]("userLookup")

	def this(universityId: String = null) {
		this()
		this.universityId = universityId
	}

	def isLate = submittedDate != null && assignment.isLate(this)
	def isAuthorisedLate = submittedDate != null && assignment.isAuthorisedLate(this)

	@ManyToOne(optional = false, cascade = Array(PERSIST, MERGE), fetch = LAZY)
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	def permissionsParents = Option(assignment).toStream

	var submitted: Boolean = false

	@Column(name = "submitted_date")
	var submittedDate: DateTime = _

	@NotNull
	var userId: String = _

	var suspectPlagiarised: JBoolean = false

	/**
	 * It isn't essential to record University ID as their user ID
	 * will identify them, but a lot of processes require the university
	 * number as the way of identifying a person and it'd be expensive
	 * to go and fetch the value every time. Also if we're keeping
	 * records for a while, the ITS account can be expunged so we'd lose
	 * it entirely.
	 */
	@NotNull
	var universityId: String = _

	@OneToMany(mappedBy = "submission", cascade = Array(ALL))
	@BatchSize(size=200)
	var values: JSet[SavedFormValue] = new HashSet

	def getValue(field: FormField): Option[SavedFormValue] = {
		values.find( _.name == field.name )
	}

	def firstMarker:Option[User] = assignment.getStudentsFirstMarker(this).map(userLookup.getUserByUserId(_))

	def secondMarker:Option[User] = assignment.getStudentsSecondMarker(this).map(userLookup.getUserByUserId(_))

	def valuesByFieldName = (values map { v => (v.name, v.value) }).toMap

	def valuesWithAttachments = values.filter(_.hasAttachments)

	def allAttachments = valuesWithAttachments.toSeq flatMap { _.attachments }

	def hasOriginalityReport: JBoolean = allAttachments.exists( _.originalityReport != null )

	def isNoteworthy: Boolean = suspectPlagiarised || isAuthorisedLate || isLate

	/** Filename as we would expect to find this attachment in a downloaded zip of submissions. */
	def zipFileName(attachment: FileAttachment) = {
		assignment.module.code + " - " + universityId + " - " + attachment.name
	}

	def zipFilename(attachment: FileAttachment, name: String) = {
		assignment.module.code + " - " + name + " - " + attachment.name
	}

	def isReleasedForMarking: Boolean = assignment.isReleasedForMarking(this)
	def isReleasedToSecondMarker: Boolean = assignment.isReleasedToSecondMarker(this)
}
