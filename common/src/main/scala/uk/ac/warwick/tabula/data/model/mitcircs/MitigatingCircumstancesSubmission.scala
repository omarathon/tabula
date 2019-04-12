package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence.CascadeType.ALL
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._

@Entity
@Access(AccessType.FIELD)
class MitigatingCircumstancesSubmission extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable
  with ToEntityReference {
    type Entity = MitigatingCircumstancesSubmission

  def this(student:StudentMember, creator:User, department: Department) {
    this()
    this.creator = creator
    this.student = student
    this.department = department
  }

  @Column(nullable = false, unique = true)
  var key: JLong = _

  @Column(nullable = false)
  var createdDate: DateTime = DateTime.now()

  @Column(nullable = false)
  var lastModified: DateTime = DateTime.now()

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  final var creator: User = _ // the user that created this

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "universityId", referencedColumnName = "universityId")
  var student: StudentMember = _

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "department_id")
  var department: Department = _

  @Column(nullable = false)
  var startDate: LocalDate = _

  @Column(nullable = true)
  var endDate: LocalDate = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.IssueTypeUserType")
  var issueType: IssueType = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var issueTypeDetails: String = _ // free text for use when the issue type is Other

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  @Column(nullable = false)
  var reason: String = _

  // No orphanRemoval as it makes it difficult to move modules between Departments.
  @OneToMany(fetch = FetchType.LAZY, cascade = Array(ALL), orphanRemoval = true)
  @JoinColumn(name = "submission_id")
  @BatchSize(size = 200)
  var affectedAssessments: JSet[MitigatingCircumstancesAffectedAssessment] = JHashSet()

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

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "student" -> student.universityId,
    "creator" -> creator.getWarwickId
  )

  // Don't use the student as the permission parent here. We don't want permissions to bubble up to all the students touchedDepartments
  override def permissionsParents: Stream[PermissionsTarget] = Stream(student.homeDepartment)
}




