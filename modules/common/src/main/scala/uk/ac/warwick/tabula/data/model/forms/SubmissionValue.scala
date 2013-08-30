package uk.ac.warwick.tabula.data.model.forms

/**
 * represents a submitted value.
 */
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.SavedSubmissionValue
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.BindingResult

/**
 * Base object for binding an individual submitted field from an assignment
 * submission request. Assignment FormFields can generate an empty SubmissionValue
 * instance that can be attached to the command for binding.
 *
 * For persisting a submission, a SavedSubmissionValue is created and persist()
 * is called to insert the specific data for saving.
 */
abstract class SubmissionValue extends BindListener {
	val field: FormField
	override def onBind(result: BindingResult) {}
	def persist(value: SavedSubmissionValue)
	
	protected def safeToString(value: Any) = Option(value).map { _.toString }.getOrElse("")
}

class StringSubmissionValue(val field: FormField) extends SubmissionValue {
	var value: String = _
	def persist(ssv: SavedSubmissionValue) { ssv.value = value }
}

class IntegerSubmissionValue(val field: FormField) extends SubmissionValue {
	var value: JInteger = _
	def persist(ssv: SavedSubmissionValue) { ssv.value = safeToString(value) }
}

class BooleanSubmissionValue(val field: FormField) extends SubmissionValue {
	var value: JBoolean = null
	def persist(ssv: SavedSubmissionValue) { ssv.value = safeToString(value) }
}

class FileSubmissionValue(val field: FormField) extends SubmissionValue {
	var file: UploadedFile = new UploadedFile

	lazy val fileDao = Wire.auto[FileDao]
	override def onBind(result: BindingResult) { file.onBind(result) }
	def persist(ssv: SavedSubmissionValue) {
		val savedAttachments = for (attachment <- file.attached) yield {
			attachment.temporary = false
			attachment.submissionValue = ssv
			attachment
		}
		ssv.attachments.clear()
		ssv.attachments.addAll(savedAttachments.toSet[FileAttachment])
	}
}