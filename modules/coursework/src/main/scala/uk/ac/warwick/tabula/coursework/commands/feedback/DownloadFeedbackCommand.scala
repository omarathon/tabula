package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConversions.asScalaBuffer
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.services.fileserver._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.permissions._


class DownloadFeedbackCommand(val module: Module, val assignment: Assignment, val feedback: Feedback) extends Command[Option[RenderableFile]] with ReadOnly {
	
	notDeleted(assignment)
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Read, feedback)
	
	var zip = Wire.auto[ZipService]
	var feedbackDao = Wire.auto[FeedbackDao]

	@BeanProperty var filename: String = _
    @BeanProperty var students: JList[String] = ArrayList()
    
	private var fileFound: Boolean = _

	var callback: (RenderableFile) => Unit = _

	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	def applyInternal() = {
		val result: Option[RenderableFile] = 
			filename match {
				case filename: String if filename.hasText => {
					feedback.attachments.find(_.name == filename).map(new RenderableAttachment(_))
				}
				case _ => Some(zipped(feedback))
			}

		fileFound = result.isDefined
		if (callback != null) {
			result.map { callback(_) }
		}
		result
	}

	private def zipped(feedback: Feedback) = new RenderableZip(zip.getFeedbackZip(feedback))

	override def describe(d: Description) = {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}