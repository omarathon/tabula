package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence._
import org.hibernate.annotations.{DynamicInsert, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._

@Entity
@DynamicInsert
@Access(AccessType.FIELD)
class MitigatingCircumstancesSubmission extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable
  with ToEntityReference {
    type Entity = MitigatingCircumstancesSubmission

  def this(student:StudentMember, creator:User) {
    this()
    this.creator = creator
    this.student = student
  }

  @Column(nullable = false)
  var key: JLong = _

  @Column(nullable = false)
  var createdDate: DateTime = DateTime.now()

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  final var creator: User = _ // the user that created this

  @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.EAGER)
  @JoinColumn(name = "universityId", referencedColumnName = "universityId")
  var student: StudentMember = _

  var startDate: DateTime = _
  var endDate: DateTime = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.IssueTypeUserType")
  var issueType: IssueType = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var issueTypeDetails: String = _ // free text for use when the issue type is Other

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var reason: String = _


  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "student" -> student.universityId
  )

  override def permissionsParents: Stream[PermissionsTarget] = ???
}




