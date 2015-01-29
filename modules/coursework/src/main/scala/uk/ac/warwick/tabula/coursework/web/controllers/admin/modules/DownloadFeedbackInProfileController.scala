package uk.ac.warwick.tabula.coursework.web.controllers.admin.modules

import org.springframework.beans.factory.annotation.Autowired
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
import uk.ac.warwick.tabula.data.model.{Module, Assignment, Member}
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController

@Controller
@RequestMapping(value = Array("/module/{module}/{assignment}/{student}"))
class DownloadFeedbackInProfileController extends CourseworkController {
	
	var feedbackDao = Wire.auto[FeedbackDao]

	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, @PathVariable("student") student: Member)
		= new DownloadFeedbackCommand(module, assignment, mandatory(feedbackDao.getFeedbackByUniId(assignment, student.universityId).filter(_.released)), student)

	@Autowired var fileServer: FileServer = _

	@RequestMapping(value = Array("/all/feedback.zip"))
	def getAll(command: DownloadFeedbackCommand, @PathVariable("student") student: Member)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		command.filename = null
		getOne(command)
	}

	@RequestMapping(value = Array("/get/{filename}"))
	def getOne(command: DownloadFeedbackCommand)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.callback = {
			(renderable) => fileServer.serve(renderable)
		}
		command.apply().orElse { throw new ItemNotFoundException() }
	}

}
