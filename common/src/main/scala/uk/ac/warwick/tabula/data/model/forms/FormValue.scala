package uk.ac.warwick.tabula.data.model.forms

/**
  * represents a submitted value.
  */
import freemarker.core.TemplateHTMLOutputModel
import javax.persistence.FetchType._
import javax.persistence._
import org.hibernate.annotations.{Cascade, CascadeType, Proxy}
import org.springframework.validation.BindingResult
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.BindListener

import scala.jdk.CollectionConverters._

/**
  * Base object for binding an individual submitted field from an assignment
  * submission request. Assignment FormFields can generate an empty SubmissionValue
  * instance that can be attached to the command for binding.
  *
  * For persisting a submission, a SavedSubmissionValue is created and persist()
  * is called to insert the specific data for saving.
  */
abstract class FormValue extends BindListener {
  val field: FormField

  override def onBind(result: BindingResult) {}

  def persist(value: SavedFormValue)

  protected def safeToString(value: Any): String = Option(value).map(_.toString).getOrElse("")
}

class StringFormValue(val field: FormField) extends FormValue {
  var value: String = _

  def persist(ssv: SavedFormValue) {
    ssv.value = value
  }
}

class IntegerFormValue(val field: FormField) extends FormValue {
  var value: JInteger = _

  def persist(ssv: SavedFormValue) {
    ssv.value = safeToString(value)
  }
}

class BooleanFormValue(val field: FormField) extends FormValue {
  var value: JBoolean = null

  def persist(ssv: SavedFormValue) {
    ssv.value = safeToString(value)
  }
}

class FileFormValue(val field: FormField) extends FormValue {
  var file: UploadedFile = new UploadedFile

  lazy val fileDao: FileDao = Wire.auto[FileDao]

  override def onBind(result: BindingResult) {
    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }

  def persist(ssv: SavedFormValue) {
    val savedAttachments = for (attachment <- file.attached.asScala) yield {
      attachment.temporary = false
      attachment.submissionValue = ssv
      attachment
    }
    ssv.attachments.clear()
    ssv.attachments.addAll(savedAttachments.toSet[FileAttachment].asJava)
  }
}

/**
  * Stores a value submitted for a single assignment or feedback field. It has
  * a few different fields to handle holding various types of item.
  * The table is called SubmissionValue for LEGACY REASONS. Schema updating
  * is left as an exercise for the Hibernate fanbois.
  */
@Entity(name = "SubmissionValue")
@Proxy
@Access(AccessType.FIELD)
class SavedFormValue extends GeneratedId with FormattedHtml {

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "submission_id")
  var submission: Submission = _

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "feedback_id")
  var feedback: Feedback = _

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "marker_feedback_id")
  var markerFeedback: MarkerFeedback = _

  // matches with assignment field name
  var name: String = _

  /**
    * Optional, only for file fields
    */
  @OneToMany(mappedBy = "submissionValue", fetch = LAZY)
  @Cascade(Array(CascadeType.PERSIST, CascadeType.MERGE, CascadeType.SAVE_UPDATE, CascadeType.REFRESH))
  var attachments: JSet[FileAttachment] = JSet()

  def hasAttachments: Boolean = attachments != null && !attachments.isEmpty

  var value: String = _

  def valueFormattedHtml: TemplateHTMLOutputModel = formattedHtml(value)
}

object SavedFormValue {
  def withAttachments(submission: Submission, name: String, attachments: Set[FileAttachment]): SavedFormValue = {
    val value = new SavedFormValue()
    value.submission = submission
    value.name = name
    value.attachments = attachments.asJava
    value
  }

  def withAttachments(feedback: Feedback, name: String, attachments: Set[FileAttachment]): SavedFormValue = {
    val value = new SavedFormValue()
    value.feedback = feedback
    value.name = name
    value.attachments = attachments.asJava
    value
  }
}
