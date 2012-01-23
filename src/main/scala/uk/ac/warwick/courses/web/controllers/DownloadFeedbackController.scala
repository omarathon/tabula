package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.commands.feedback.DownloadFeedbackCommand
import uk.ac.warwick.courses.commands.feedback.AdminGetSingleFeedbackCommand
import uk.ac.warwick.courses.services.fileserver.FileServer
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.ItemNotFoundException
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.actions.Participate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.actions.View
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.data.FeedbackDao
import org.springframework.web.bind.annotation.ModelAttribute

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}"))
class DownloadFeedbackController extends AbstractAssignmentController {

	@ModelAttribute def command(user:CurrentUser) = new DownloadFeedbackCommand(user)
    
	@Autowired var fileServer:FileServer =_
	
	@RequestMapping(value=Array("/all/feedback.zip"), method=Array(RequestMethod.GET))
	def getAll(command:DownloadFeedbackCommand, user:CurrentUser, response:HttpServletResponse):Unit = {
		command.filename = null
		getOne(command,user,response)
	}
	
	@RequestMapping(value=Array("/get/{filename}"), method=Array(RequestMethod.GET))
	def getOne(command:DownloadFeedbackCommand, user:CurrentUser, response:HttpServletResponse):Unit = {
		mustBeLinked(command.assignment, command.module)
		
		// Does permission checks.
		checkCanGetFeedback(command.assignment, user)
		
		command.apply() match {
		  case Some(renderable) => {
		 	  fileServer.serve(renderable, response)
		  }
		  case None => throw new ItemNotFoundException
		}
	}

}