package uk.ac.warwick.courses.commands.feedback

import scala.collection.JavaConversions.asScalaBuffer
import scala.reflect.BeanProperty

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable

import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data._
import uk.ac.warwick.courses.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.courses.services.fileserver._
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.spring.Wire

class DownloadFeedbackCommand(user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly {
	var zip = Wire.auto[ZipService]
	var feedbackDao = Wire.auto[FeedbackDao]

	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _

	var callback: (RenderableFile) => Unit = _

	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	def work() = {
		val result: Option[RenderableFile] = feedbackDao.getFeedbackByUniId(assignment, user.universityId) flatMap { (feedback) =>
			filename match {
				case filename: String if filename.hasText => {
					feedback.attachments.find(_.name == filename).map(new RenderableAttachment(_))
				}
				case _ => Some(zipped(feedback))
			}
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