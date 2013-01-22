package uk.ac.warwick.tabula.coursework.commands.assignments

import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.data.model.forms.Extension

class DownloadSupportingFilesCommand(val module: Module, val assignment: Assignment, val extension: Extension, val filename: String) extends Command[Option[RenderableFile]] with ReadOnly{
	
	mustBeLinked(mandatory(assignment), mandatory(module))

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal() = {
		val allAttachments = extension.nonEmptyAttachments
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

class AdminDownloadSupportingFilesCommand(module: Module, assignment: Assignment, extension: Extension, filename: String) 
	extends DownloadSupportingFilesCommand(module, assignment, extension, filename) {
	
	PermissionsCheck(Manage(mandatory(extension)))
}
