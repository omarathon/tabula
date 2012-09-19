package uk.ac.warwick.courses.data.model.forms

/**
 * represents a submitted value.
 */
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.commands.UploadedFile
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.data.model.SavedSubmissionValue
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.courses.data.model.FileAttachment

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
@Configurable
class FileSubmissionValue(val field: FormField) extends SubmissionValue {
	@BeanProperty var file: UploadedFile = new UploadedFile

	// Is it too much to invoke autowire for every FileSubmissionValue created?
	// Perhaps manually provide a persistence helper object to all SubmissionValues,
	// which includes a method to make temporary files permanent.

	@Autowired var fileDao: FileDao = _
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