package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.{InvestigationCompleted, SuspectPlagiarised}
import uk.ac.warwick.tabula.data.model.forms.{FormField, SavedFormValue}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

object Submission {
	val UseDisabilityFieldName = "use-disability"
}

@Entity @Access(AccessType.FIELD)
class Submission extends GeneratedId with PermissionsTarget with ToEntityReference with FeedbackReportGenerator {

	type Entity = Submission

	def this(universityId: String = null) {
		this()
		this.universityId = universityId
	}

	def isLate = submittedDate != null && assignment.isLate(this)
	def isAuthorisedLate = submittedDate != null && assignment.isAuthorisedLate(this)
	def workingDaysLate = assignment.workingDaysLate(this)
	def deadline = assignment.submissionDeadline(this)

	def feedbackDeadline = assignment.feedbackDeadlineForSubmission(this)

	def feedbackDeadlineWorkingDaysAway = assignment.feedbackDeadlineWorkingDaysAwayForSubmission(this)

	@ManyToOne(optional = false, cascade = Array(PERSIST, MERGE), fetch = LAZY)
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	def permissionsParents = Option(assignment).toStream

	var submitted: Boolean = false

	@Column(name = "submitted_date")
	var submittedDate: DateTime = _

	@NotNull
	var userId: String = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.PlagiarismInvestigationUserType")
	var plagiarismInvestigation: PlagiarismInvestigation = PlagiarismInvestigation.Default

	def suspectPlagiarised = plagiarismInvestigation == SuspectPlagiarised
	def investigationCompleted = plagiarismInvestigation == InvestigationCompleted

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
	var values: JSet[SavedFormValue] = new java.util.HashSet

	def getValue(field: FormField): Option[SavedFormValue] = {
		values.find( _.name == field.name )
	}

	def isForUser(user: User) = universityId == user.getWarwickId || userId == user.getUserId

	def firstMarker:Option[User] = assignment.getStudentsFirstMarker(universityId)

	def secondMarker:Option[User] = assignment.getStudentsSecondMarker(universityId)

	def valuesByFieldName = (values map { v => (v.name, v.value) }).toMap

	def valuesWithAttachments = values.filter(_.hasAttachments)

	def allAttachments = valuesWithAttachments.toSeq flatMap { _.attachments }

	def hasOriginalityReport: JBoolean = allAttachments.exists( _.originalityReportReceived )

	def isNoteworthy: Boolean = suspectPlagiarised || isAuthorisedLate || isLate

	/** Filename as we would expect to find this attachment in a downloaded zip of submissions. */
	def zipFileName(attachment: FileAttachment) = {
		assignment.module.code + " - " + universityId + " - " + attachment.name
	}

	def zipFilename(attachment: FileAttachment, name: String) = {
		assignment.module.code + " - " + name + " - " + attachment.name
	}

	def useDisability: Boolean = values.find(_.name == Submission.UseDisabilityFieldName).exists(_.value.toBoolean)

	def toEntityReference = new SubmissionEntityReference().put(this)
}

trait FeedbackReportGenerator {
	def universityId: String
	def feedbackDeadline: Option[LocalDate]
}