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
@RequestMapping(value=Array("/module/{module}/{assignment}/feedback.zip"))
class DownloadFeedbackController extends BaseController {

	@ModelAttribute def command(user:CurrentUser) = new DownloadFeedbackCommand(user)
    
	@Autowired var fileServer:FileServer =_
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def get(command:DownloadFeedbackCommand, user:CurrentUser, response:HttpServletResponse) {
		mustBeLinked(command.assignment, command.module)
		mustBeAbleTo(View(command.module))
		command.apply() match {
		  case Some(zip) => fileServer.serve(zip, response)
		  case None => throw new ItemNotFoundException
		}
	}

}