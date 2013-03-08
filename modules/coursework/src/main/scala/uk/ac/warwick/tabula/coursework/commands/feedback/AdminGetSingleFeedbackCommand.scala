package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.{MarkerFeedback, Feedback}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import scala.reflect.BeanProperty

class AdminGetSingleFeedbackCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[RenderableZip] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Read, feedback)
	
	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
		val zip = zipService.getFeedbackZip(feedback)
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.feedback(feedback).properties(
		"studentId" -> feedback.universityId,
		"attachmentCount" -> feedback.attachments.size)
}

class AdminGetSingleFeedbackFileCommand(module: Module, assignment: Assignment, feedback: Feedback) extends Command[Option[RenderableFile]] with ReadOnly {
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Read, feedback)
	
	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _
	
	var callback: (RenderableFile) => Unit = _

	def applyInternal() = {
		val thing = feedback.attachments
		val attachment = Option(new RenderableAttachment(feedback.attachments.get(0)))

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



class AdminGetSingleMarkerFeedbackCommand(module: Module, assignment: Assignment, markerFeedback: MarkerFeedback) extends Command[RenderableZip] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Create, assignment)

	var zipService = Wire.auto[ZipService]

	override def applyInternal() = {
		val zip = zipService.getSomeMarkerFeedbacksZip(Seq(markerFeedback))
		new RenderableZip(zip)
	}

	override def describe(d: Description) = d.feedback(markerFeedback.feedback).properties(
		"studentId" -> markerFeedback.feedback.universityId,
		"attachmentCount" -> markerFeedback.attachments.size)
}