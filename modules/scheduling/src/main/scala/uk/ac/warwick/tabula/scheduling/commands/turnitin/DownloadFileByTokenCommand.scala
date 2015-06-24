package uk.ac.warwick.tabula.scheduling.commands.turnitin

import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.permissions.Public

class DownloadFileByTokenCommand(
		val submission: Submission,
		val fileAttachment: FileAttachment,
		val token: FileAttachmentToken )
		extends Command[Option[RenderableFile]] with ReadOnly with Public {

	mustBeLinked(mandatory(fileAttachment), mandatory(submission))

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal() = {

		val attachment = Option(new RenderableAttachment(fileAttachment))

		fileFound = attachment.isDefined
		if (callback != null) {
			attachment.map { callback(_) }
		}
		attachment

	}

	override def describe(d: Description) = {
		d.submission(submission)
		d.fileAttachments(Seq(fileAttachment))
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}