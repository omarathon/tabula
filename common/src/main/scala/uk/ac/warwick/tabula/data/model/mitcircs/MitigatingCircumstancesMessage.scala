package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence.CascadeType._
import javax.persistence._
import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

@Entity
@Access(AccessType.FIELD)
class MitigatingCircumstancesMessage extends GeneratedId
  with ToString
  with Serializable
  with ToEntityReference {
    type Entity = MitigatingCircumstancesMessage

  def this(submission:MitigatingCircumstancesSubmission, sender:User) {
    this()
    this.sender = sender
    this.submission = submission
  }

  @Column(nullable = false)
  var submission: MitigatingCircumstancesSubmission = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.EncryptedStringUserType")
  var message: String = _ // free text for use when the issue type includes Other

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  final var sender: User = _

  @Column(nullable = false)
  var createdDate: DateTime = DateTime.now()

  @OneToMany(mappedBy = "mitigatingCircumstancesSubmission", fetch = FetchType.LAZY, cascade = Array(ALL))
  @BatchSize(size = 200)
  var attachments: JSet[FileAttachment] = JHashSet()

  def addAttachment(attachment: FileAttachment) {
    if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
    attachment.temporary = false
    attachment.mitigatingCircumstancesMessage = this
    attachments.add(attachment)
  }

  def removeAttachment(attachment: FileAttachment): Boolean = {
    attachment.mitigatingCircumstancesMessage = null
    attachments.remove(attachment)
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "submission" -> submission.key,
    "sender" -> sender.getWarwickId
  )
}




