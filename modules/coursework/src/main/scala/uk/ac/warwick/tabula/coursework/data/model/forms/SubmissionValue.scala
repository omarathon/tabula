package uk.ac.warwick.tabula.coursework.data.model.forms

/**
 * represents a submitted value.
 */
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.commands.UploadedFile
import uk.ac.warwick.tabula.coursework.data.FileDao
import uk.ac.warwick.tabula.coursework.data.model.SavedSubmissionValue
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.data.model.FileAttachment
import uk.ac.warwick.spring.Wire

/**
 * Base object for binding an individual submitted field from an assignment
 * submission request. Assignment FormFields can generate an empty SubmissionValue
 * instance that can be attached to the command for binding.
 *
 * For persisting a submission, a SavedSubmissionValue is created and persist()
 * is called to insert the specific data for saving.
 */
abstract class SubmissionValue {
	val field: FormField
	def onBind {}
	def persist(value: SavedSubmissionValue)
}

class StringSubmissionValue(val field: FormField) extends SubmissionValue {
	@BeanProperty var value: String = _
	def persist(ssv: SavedSubmissionValue) { ssv.value = value }
}
class BooleanSubmissionValue(val field: FormField) extends SubmissionValue {
	@BeanProperty var value: JBoolean = null
	def persist(ssv: SavedSubmissionValue) { ssv.value = Option(value).map { _.toString }.getOrElse("") }
}

class FileSubmissionValue(val field: FormField) extends SubmissionValue {
	@BeanProperty var file: UploadedFile = new UploadedFile

	lazy val fileDao = Wire.auto[FileDao]
	override def onBind { file.onBind }
	def persist(ssv: SavedSubmissionValue) {
		val savedAttachments = for (attachment <- file.attached) yield {
			attachment.temporary = false
			attachment.submissionValue = ssv
			attachment
		}
		ssv.attachments = savedAttachments.toSet[FileAttachment]
	}
}