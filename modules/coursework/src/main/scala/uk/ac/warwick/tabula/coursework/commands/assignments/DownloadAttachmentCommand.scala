package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService

class DownloadAttachmentCommand(
		val module: Module, 
		val assignment: Assignment, 
		val submission: Submission,
		val student: Member)
		extends Command[Option[RenderableFile]] with ReadOnly {

	var userLookup = Wire[UserLookupService]
	
	mustBeLinked(mandatory(assignment), mandatory(module))

	PermissionCheckAny(
		Seq(CheckablePermission(Permissions.Submission.Read, submission),
			CheckablePermission(Permissions.Submission.Read, student))
	)


	var filename: String = _

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