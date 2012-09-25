package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.fileserver.FileServer
import scala.Array
import uk.ac.warwick.courses.commands.assignments.{DownloadSupportingFilesCommand, DownloadAttachmentCommand}
import uk.ac.warwick.courses.{ItemNotFoundException, CurrentUser}
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.web.controllers.BaseController

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/extensions/review-request/{universityId}"))
class DownloadSupportingFilesController extends BaseController{

	@ModelAttribute def command(user:CurrentUser) = new DownloadSupportingFilesCommand(user)

	@Autowired var fileServer:FileServer =_

	@RequestMapping(value=Array("/supporting-file/{filename}"), method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command:DownloadSupportingFilesCommand, user:CurrentUser, response:HttpServletResponse) = {

		mustBeLinked(mandatory(command.assignment), mandatory(command.module))

		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable, response)	}
		command.apply().orElse{ throw new ItemNotFoundException() }
	}

}
