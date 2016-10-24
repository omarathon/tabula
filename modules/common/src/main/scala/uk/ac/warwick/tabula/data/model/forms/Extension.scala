package uk.ac.warwick.tabula.data.model.forms

import uk.ac.warwick.tabula.data.model.forms.ExtensionState.Approved

import scala.collection.JavaConversions._
import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.{DateTime, Days}
import javax.persistence._
import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.validation.constraints.NotNull

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl
import uk.ac.warwick.userlookup.User
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

import uk.ac.warwick.tabula.DateFormats
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.system.TwoWayConverter

@Entity @Access(AccessType.FIELD)
class Extension extends GeneratedId with PermissionsTarget with ToEntityReference {

	type Entity = Extension

	def this(universityId:String=null) {
		this()
		this.universityId = universityId
	}

	@ManyToOne(optional=false, cascade=Array(PERSIST,MERGE), fetch=FetchType.LAZY)
	@JoinColumn(name="assignment_id")
	var assignment:Assignment = _

	def permissionsParents = Option(assignment).toStream

	@NotNull
	var userId: String = _

	@NotNull
	var universityId: String = _

	def isForUser(user: User): Boolean = isForUser(user.getWarwickId, user.getUserId)
	def isForUser(theUniversityId: String, theUsercode: String): Boolean = universityId == theUniversityId || userId == theUsercode

	// TODO should there be a single def that returns the expiry date for approved/manual extensions, and requested expiry date otherwise?
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@Column(name = "requestedExpiryDate")
	private var _requestedExpiryDate: DateTime = _
	def requestedExpiryDate: Option[DateTime] = Option(_requestedExpiryDate)
	def requestedExpiryDate_=(red: DateTime) {_requestedExpiryDate = red}

	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@Column(name = "expiryDate")
	private var _expiryDate: DateTime = _

	def expiryDate_=(ed: DateTime) {_expiryDate = ed}

	def expiryDate: Option[DateTime] = {
		if (_expiryDate == null || state != Approved) None
		else Some(_expiryDate)
	}

	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var requestedOn: DateTime = _
	@Type(`type` = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	@Column(name = "approvedOn")
	var reviewedOn: DateTime = _

	var reason: String = _
	@Column(name = "approvalComments")
	var reviewerComments: String = _
	var disabilityAdjustment: Boolean = false

	@Column(name = "state")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.forms.ExtensionStateUserType")
	var _state: ExtensionState = ExtensionState.Unreviewed
	def state = _state
	/** Don't use rawState_ directly, call approve() or reject() instead **/
	def rawState_= (state: ExtensionState) { _state = state }


	@OneToMany(mappedBy = "extension", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var attachments: JSet[FileAttachment] = JSet()

	def nonEmptyAttachments = attachments.toSeq filter(_.hasData)

	def addAttachment(attachment: FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.extension = this
		attachments.add(attachment)
	}

	def removeAttachment(attachment: FileAttachment) = {
		attachment.extension = null
		attachments.remove(attachment)
	}

	// this extension was manually created by an administrator, rather than requested by a student
	def isManual = requestedOn == null
	def isInitiatedByStudent = !isManual

	// you can't infer from state alone whether there's a request outstanding - use awaitingReview()
	def approved = state == ExtensionState.Approved
	def rejected = state == ExtensionState.Rejected
	def unreviewed = state == ExtensionState.Unreviewed
	def revoked = state == ExtensionState.Revoked
	def moreInfoRequired = state == ExtensionState.MoreInformationRequired

	def rejectable = awaitingReview || (approved && isInitiatedByStudent)
	def revocable = approved && !isInitiatedByStudent

	def updateState(newState: ExtensionState, comments: String) = {
		_state = newState
		reviewedOn = DateTime.now
		reviewerComments = comments
	}

	// keep state encapsulated
	def approve(comments: String = null) = updateState(ExtensionState.Approved, comments)
	def reject(comments: String = null) = updateState(ExtensionState.Rejected, comments)
	def revoke(comments: String = null) = updateState(ExtensionState.Revoked, comments)
	def requestMoreInfo(comments: String = null) = updateState(ExtensionState.MoreInformationRequired, comments)

	def awaitingReview = {
		// wrap nullable dates to be more readable in pattern match
		val requestDate = Option(requestedOn)
		val reviewDate = Option(reviewedOn)

		(requestDate, reviewDate) match {
			case (Some(request), None) => true
			case (Some(latestRequest), Some(lastReview)) if latestRequest.isAfter(lastReview) => true
			case _ => false
		}
	}

	@transient
	lazy val workingDaysHelper = new WorkingDaysHelperImpl

	// calculate deadline only if not late (return None for late returns)
	def feedbackDeadline = assignment
		.findSubmission(universityId)
		.flatMap(s => expiryDate.map(_.isAfter(s.submittedDate)))
		.collect {
			case(true) => feedbackDueDate
		}.flatten

	// the feedback deadline if an expry date exists for this extension
	def feedbackDueDate = expiryDate.map(ed =>
		workingDaysHelper.datePlusWorkingDays(ed.toLocalDate, Feedback.PublishDeadlineInWorkingDays).toDateTime(ed)
	)


	def toEntityReference = new ExtensionEntityReference().put(this)

	def duration = expiryDate.map(Days.daysBetween(assignment.closeDate, _).getDays).getOrElse(0)

	def requestedExtraDuration = requestedExpiryDate
		.map(Days.daysBetween(expiryDate.getOrElse(assignment.closeDate), _).getDays).getOrElse(0)
}


object Extension {
	val MaxDaysToDisplayAsProgressBar = 8 * 7
}


sealed abstract class ExtensionState(val dbValue: String, val description: String)

object ExtensionState {
	// you can't infer from state alone whether there's a request outstanding - use extension.awaitingReview()
	case object Unreviewed extends ExtensionState("U", "Unreviewed")
	case object Approved extends ExtensionState("A", "Approved")
	case object MoreInformationRequired extends ExtensionState("M", "More information required")
	case object Rejected extends ExtensionState("R", "Rejected")
	case object Revoked extends ExtensionState("V", "Revoked")

	def fromCode(code: String) = code match {
		case Unreviewed.dbValue => Unreviewed
		case Approved.dbValue => Approved
		case MoreInformationRequired.dbValue => MoreInformationRequired
		case Rejected.dbValue => Rejected
		case Revoked.dbValue => Revoked
		case _ => throw new IllegalArgumentException()
	}

	def all = Seq(Unreviewed, Approved, Rejected, Revoked, MoreInformationRequired)
}

class ExtensionStateUserType extends AbstractBasicUserType[ExtensionState, String] {
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = ExtensionState.fromCode(string)
	override def convertToValue(es: ExtensionState) = es.dbValue
}

class ExtensionStateConverter extends TwoWayConverter[String, ExtensionState] {
	override def convertRight(code: String) = ExtensionState.fromCode(code)
	override def convertLeft(state: ExtensionState) = (Option(state) map { _.dbValue }).orNull
}