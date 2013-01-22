package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.reflect.BeanProperty
import scala.collection.JavaConversions.asScalaBuffer
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.AssignmentService
import scala.None
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.actions.View

class DownloadAttachmentCommand(val module: Module, val assignment: Assignment, val submission: Submission) extends Command[Option[RenderableFile]] with ReadOnly {
	
	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionsCheck(View(submission))

	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal() = {
		val attachment = submission.allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

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