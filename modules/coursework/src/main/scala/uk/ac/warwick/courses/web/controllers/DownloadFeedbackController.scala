package uk.ac.warwick.courses.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.commands.feedback.DownloadFeedbackCommand
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import org.springframework.web.bind.annotation.RequestMethod

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}"))
class DownloadFeedbackController extends AbstractAssignmentController {

	@ModelAttribute def command(user: CurrentUser) = new DownloadFeedbackCommand(user)

	@Autowired var fileServer: FileServer = _

	@RequestMapping(value = Array("/all/feedback.zip"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAll(command: DownloadFeedbackCommand, user: CurrentUser, response: HttpServletResponse): Unit = {
		command.filename = null
		getOne(command, user, response)
	}

	@RequestMapping(value = Array("/get/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getOne(command: DownloadFeedbackCommand, user: CurrentUser, response: HttpServletResponse): Unit = {
		mustBeLinked(command.assignment, command.module)

		// Does permission checks.
		checkCanGetFeedback(command.assignment, user)

		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable, response) }
		command.apply().orElse { throw new ItemNotFoundException() }
	}

}