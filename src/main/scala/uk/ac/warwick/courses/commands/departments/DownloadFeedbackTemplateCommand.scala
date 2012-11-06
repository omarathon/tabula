package uk.ac.warwick.courses.commands.departments

import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.{Description, ReadOnly, Command}
import uk.ac.warwick.courses.services.fileserver.{RenderableAttachment, RenderableFile}
import reflect.BeanProperty
import uk.ac.warwick.courses.data.model.{Department, FeedbackTemplate}

class DownloadFeedbackTemplateCommand(user: CurrentUser) extends Command[Option[RenderableFile]] with ReadOnly{

	@BeanProperty var template: FeedbackTemplate = _
	@BeanProperty var department: Department = _
	@BeanProperty var filename: String = _

	private var fileFound: Boolean = _
	var callback: (RenderableFile) => Unit = _

	def apply() = {

		val attachment = Option(template.attachment)
		val renderableAttachment = attachment find (_.name == filename) map (a => new RenderableAttachment(a))

		fileFound = renderableAttachment.isDefined
		if (callback != null) {
			renderableAttachment.map { callback(_) }
		}
		renderableAttachment
	}

	override def describe(d: Description) {
		d.department(department)
		d.property("template", template.id)
	}

	override def describeResult(d: Description) {
		d.property("fileFound", fileFound)
	}

}
