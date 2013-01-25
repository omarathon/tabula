package uk.ac.warwick.tabula.coursework.commands.departments

import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.FeedbackTemplate
import uk.ac.warwick.tabula.actions.Manage


class DownloadFeedbackTemplateCommand(val department: Department, user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly{
	
	PermissionsCheck(Manage(department))

	@BeanProperty var template: FeedbackTemplate = _
	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def applyInternal() = {

		val attachment = Option(template.attachment)
		val renderableAttachment = attachment find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = renderableAttachment.isDefined
		if (callback != null) {
			renderableAttachment.map { callback(_) }
		}
		renderableAttachment
	}

	override def describe(d: Description) = d
		.department(department)
		.property("template", template.id)

	override def describeResult(d: Description) = d
		.property("fileFound", fileFound)

}
