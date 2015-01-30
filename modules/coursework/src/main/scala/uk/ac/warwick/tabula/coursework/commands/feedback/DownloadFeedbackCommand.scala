package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConversions.asScalaBuffer
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.services.fileserver._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._

class DownloadFeedbackCommand(val module: Module, val assignment: Assignment, val feedback: Feedback, val student: Option[Member])
	extends Command[Option[RenderableFile]] with HasCallback[RenderableFile] with ReadOnly {

	notDeleted(assignment)
	mustBeLinked(assignment, module)

	student match {
		case Some(student: StudentMember) => PermissionCheckAny(
			Seq(CheckablePermission(Permissions.Feedback.Read, feedback),
				CheckablePermission(Permissions.Submission.Read, student))
		)
		case _ => PermissionCheck(Permissions.Feedback.Read, feedback)
	}

	var zip = Wire.auto[ZipService]
	var feedbackDao = Wire.auto[FeedbackDao]

	var filename: String = _
    var students: JList[String] = JArrayList()

	/**
	 * If filename is unset, it returns a renderable Zip of all files.
	 * If filename is set, it will return a renderable attachment if found.
	 * In either case if it's not found, None is returned.
	 */
	def applyInternal() = {
		val result: Option[RenderableFile] = 
			filename match {
				case filename: String if filename.hasText =>
					feedback.attachments.find(_.name == filename).map(new RenderableAttachment(_))
				case _ => Some(zipped(feedback))
			}

		if (callback != null) {
			result.map { callback }
		}
		result
	}

	private def zipped(feedback: Feedback) = new RenderableZip(zip.getFeedbackZip(feedback))

	override def describe(d: Description) = {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description, result: Option[RenderableFile]) {
		d.property("fileFound", result.isDefined)
	}

}