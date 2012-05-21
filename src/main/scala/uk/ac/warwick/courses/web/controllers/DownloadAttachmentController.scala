package uk.ac.warwick.courses.web.controllers

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.services.fileserver.FileServer
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.DownloadAttachmentCommand
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.commands.feedback.DownloadFeedbackCommand
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.ItemNotFoundException

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}"))
class DownloadAttachmentController extends AbstractAssignmentController{

	@ModelAttribute def command(user:CurrentUser) = new DownloadAttachmentCommand(user)
  
	@Autowired var fileServer:FileServer =_
  
	@RequestMapping(value=Array("/attachment/{filename}"), method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command:DownloadAttachmentCommand, user:CurrentUser, response:HttpServletResponse):Unit = {
		mustBeLinked(command.assignment, command.module)
		
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable, response)	}
		command.apply().orElse{ throw new ItemNotFoundException() }
	}

}