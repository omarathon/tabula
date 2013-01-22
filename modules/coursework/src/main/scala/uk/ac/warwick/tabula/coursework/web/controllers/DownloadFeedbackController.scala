package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.coursework.commands.feedback.DownloadFeedbackCommand
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import org.springframework.web.bind.annotation.RequestMethod
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}"))
class DownloadFeedbackController extends CourseworkController {
	
	var feedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, user: CurrentUser) 
		= new DownloadFeedbackCommand(module, assignment, mandatory(feedbackDao.getFeedbackByUniId(assignment, user.universityId).filter(_.released)))

	@Autowired var fileServer: FileServer = _

	@RequestMapping(value = Array("/all/feedback.zip"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAll(command: DownloadFeedbackCommand, user: CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		command.filename = null
		getOne(command, user)
	}

	@RequestMapping(value = Array("/get/{filename}"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getOne(command: DownloadFeedbackCommand, user: CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable) }
		command.apply().orElse { throw new ItemNotFoundException() }
	}

}