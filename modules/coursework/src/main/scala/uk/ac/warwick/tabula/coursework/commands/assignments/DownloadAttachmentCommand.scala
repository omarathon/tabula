package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.reflect.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.Command
import uk.ac.warwick.tabula.coursework.commands.Description
import uk.ac.warwick.tabula.coursework.commands.ReadOnly
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.data.model.Submission
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.coursework.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.coursework.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.coursework.services.AssignmentService
import scala.None
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire

class DownloadAttachmentCommand(user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly {
  var assignmentService = Wire.auto[AssignmentService]

	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def work() = {
		val submission = assignmentService.getSubmissionByUniId(assignment, user.universityId);
		val allAttachments = submission map { _.allAttachments } getOrElse Nil
		val attachment = allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = attachment.isDefined
		if (callback != null) {
			attachment.map { callback(_) }
		}
		attachment
	}

	override def describe(d: Description) = {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}