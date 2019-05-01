package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

@Entity
@Access(AccessType.FIELD)
class MitigatingCircumstancesSubmission extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable
  with ToEntityReference
  with FormattedHtml {
    type Entity = MitigatingCircumstancesSubmission

  def this(student: StudentMember, creator: User, department: Department) {
    this()
    this.creator = creator
    this.lastModifiedBy = creator
    this.student = student
    this.department = department
  }

  @Column(nullable = false, unique = true)
  var key: JLong = _

  @Column(nullable = false)
  var createdDate: DateTime = DateTime.now()

  @Column(name = "lastModified", nullable = false)
  private var _lastModified: DateTime = DateTime.now()

  /**
    * This will return the latest of:
    * - the submission's last modified date
    * - the latest message that was sent
    */
  def lastModified: DateTime =
    Seq(
      Option(_lastModified),
      messages.sortBy(_.createdDate).lastOption.map(_.createdDate)
    ).flatten.max

  def lastModified_=(lastModified: DateTime): Unit = _lastModified = lastModified

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  final var creator: User = _ // the user that created this

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  var lastModifiedBy: User = _

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "universityId", referencedColumnName = "universityId")
  var student: StudentMember = _

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @ManyToOne(cascade = Array(ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "relatedSubmission")
  var relatedSubmission: MitigatingCircumstancesSubmission = _

  @Column(nullable = false)
  var startDate: LocalDate = _

  @Column(nullable = true)
  var endDate: LocalDate = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.IssueTypeUserType")
  var issueTypes: Seq[IssueType] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var issueTypeDetails: String = _ // free text for use when the issue type includes Other

  var contacted: JBoolean = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitCircsContactUserType")
  var contacts: Seq[MitCircsContact] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var contactOther: String = _ // free text for use when the contacts includes Other

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var noContactReason: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(nullable = false)
  var reason: String = _

  def formattedReason: String = formattedHtml(reason)

  @OneToMany(fetch = FetchType.LAZY, cascade = Array(ALL), orphanRemoval = true)
  @JoinColumn(name = "submission_id")
  @BatchSize(size = 200)
  @OrderBy("academicYear, moduleCode, sequence")
  var affectedAssessments: JList[MitigatingCircumstancesAffectedAssessment] = JArrayList()

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(nullable = false)
  var pendingEvidence: String = _

  def formattedPendingEvidence: String = formattedHtml(pendingEvidence)

  var pendingEvidenceDue: LocalDate = _

  @Column(name = "approvedOn")
  private var _approvedOn: DateTime = _

  // Guarded against being set directly; use .approveAndSubmit() below
  def approvedOn: Option[DateTime] = Option(_approvedOn)

  @OneToMany(mappedBy = "mitigatingCircumstancesSubmission", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  var attachments: JSet[FileAttachment] = JHashSet()

  def addAttachment(attachment: FileAttachment) {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.mitigatingCircumstancesSubmission = this
    attachments.add(attachment)
  }

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachment.mitigatingCircumstancesSubmission = null
    attachments.remove(attachment)
  }

  @OneToMany(mappedBy = "submission", fetch = FetchType.LAZY, cascade = Array(ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  @OrderBy("createdDate")
  private var _messages: JList[MitigatingCircumstancesMessage] = JArrayList()
  def messages: Seq[MitigatingCircumstancesMessage] = _messages.asScala

  // Intentionally no default here, rely on a state being set explicitly
  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmissionStateUserType")
  @Column(name = "state", nullable = false)
  private var _state: MitigatingCircumstancesSubmissionState = _

  // Can get but not set state directly, use the methods below to transition
  def state: MitigatingCircumstancesSubmissionState = _state

  // State transitions
  def saveAsDraft(): Unit = {
    // TODO guard against doing this from invalid states

    _state = MitigatingCircumstancesSubmissionState.Draft
    _approvedOn = null
  }

  def saveOnBehalfOfStudent(): Unit = {
    // TODO guard against doing this from invalid states

    _state = MitigatingCircumstancesSubmissionState.CreatedOnBehalfOfStudent
    _approvedOn = null
  }

  def approveAndSubmit(): Unit = {
    // TODO guard against doing this from invalid states

    _state = MitigatingCircumstancesSubmissionState.Submitted
    _approvedOn = DateTime.now()
  }

  def isDraft: Boolean = state == MitigatingCircumstancesSubmissionState.Draft
  def isEditable: Boolean =
    state == MitigatingCircumstancesSubmissionState.Draft ||
    state == MitigatingCircumstancesSubmissionState.CreatedOnBehalfOfStudent ||
    state == MitigatingCircumstancesSubmissionState.Submitted

  def hasEvidence: Boolean = !attachments.isEmpty
  def isEvidencePending: Boolean = pendingEvidenceDue != null

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "student" -> student.universityId,
    "creator" -> creator.getWarwickId
  )

  // Don't use the student directly as the permission parent here. We don't want permissions to bubble up to all the students touchedDepartments
  override def permissionsParents: Stream[PermissionsTarget] = Stream(MitigatingCircumstancesStudent(student))
}




