package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, ModelAttribute}
import uk.ac.warwick.courses.{ItemNotFoundException, CurrentUser}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.fileserver.FileServer
import scala.Array
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.commands.departments.DownloadFeedbackTemplateCommand

@Controller
@RequestMapping(Array("/admin/department/{department}/settings/feedback-templates/download"))
class DownloadFeedbackTemplateController extends BaseController {

	@ModelAttribute def command(user:CurrentUser) = new DownloadFeedbackTemplateCommand(user)
	@Autowired var fileServer:FileServer =_

	@RequestMapping(value=Array("{template}/{filename}") ,method = Array(GET, HEAD))
	def getAttachment(command:DownloadFeedbackTemplateCommand, user:CurrentUser, response:HttpServletResponse) = {
		// specify callback so that audit logging happens around file serving
		command.callback = {(renderable) => fileServer.serve(renderable, response)}
		command.apply().orElse{ throw new ItemNotFoundException() }
	}

}
