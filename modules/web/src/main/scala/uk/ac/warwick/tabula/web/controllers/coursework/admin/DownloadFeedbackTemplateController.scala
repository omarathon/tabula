package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.coursework.departments.DownloadFeedbackTemplateCommand
import uk.ac.warwick.tabula.web.controllers.coursework.CourseworkController
import uk.ac.warwick.tabula.data.model.{Department, FeedbackTemplate}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

@Controller
@RequestMapping(Array("/coursework/admin/department/{department}/settings/feedback-templates/download/{template}/{filename}"))
class DownloadFeedbackTemplateController extends CourseworkController {

	@ModelAttribute def command(
		@PathVariable department: Department,
		@PathVariable template: FeedbackTemplate,
		@PathVariable filename: String,
		user:CurrentUser) =
			new DownloadFeedbackTemplateCommand(department, template, filename, user)

	@RequestMapping(method = Array(GET, HEAD))
	def getAttachment(command: DownloadFeedbackTemplateCommand, user:CurrentUser): RenderableFile = {
		command.apply().getOrElse{ throw new ItemNotFoundException() }
	}

}
