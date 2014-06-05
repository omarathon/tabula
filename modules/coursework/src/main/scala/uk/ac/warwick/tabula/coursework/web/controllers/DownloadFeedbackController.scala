package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.coursework.commands.feedback.DownloadFeedbackCommand
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.FeedbackService

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}"))
class DownloadFeedbackController extends CourseworkController {
	
	var feedbackService = Wire[FeedbackService]
	var fileServer = Wire[FileServer]

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) 
		= new DownloadFeedbackCommand(module, assignment, mandatory(feedbackService.getFeedbackByUniId(assignment, user.universityId).filter(_.released)))

	@RequestMapping(value = Array("/all/feedback.zip"))
	def getAll(command: DownloadFeedbackCommand, user: CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		command.filename = null
		getOne(command, user)
	}

	@RequestMapping(value = Array("/get/{filename}"))
	def getOne(command: DownloadFeedbackCommand, user: CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable) }
		command.apply().orElse { throw new ItemNotFoundException() }
	}

}
