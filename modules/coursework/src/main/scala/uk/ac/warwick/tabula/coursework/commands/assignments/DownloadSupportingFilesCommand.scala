package uk.ac.warwick.tabula.coursework.commands.assignments

import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser


class DownloadSupportingFilesCommand(user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly{

	@BeanProperty var module: Module = _
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal() = {
		val extension = assignment.findExtension(user.universityId)
		val allAttachments = extension map {_.nonEmptyAttachments} getOrElse Nil
		val attachment = allAttachments find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = attachment.isDefined
		if (callback != null) {
			attachment.map { callback(_) }
		}
		attachment
	}

	override def describe(d: Description) {
		d.assignment(assignment)
		d.property("filename", filename)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}
