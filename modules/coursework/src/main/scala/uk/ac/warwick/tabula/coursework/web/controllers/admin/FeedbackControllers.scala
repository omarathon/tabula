package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.AuditEventIndexService
import uk.ac.warwick.tabula.services.fileserver.FileServer
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/download/{feedbackId}/{filename}"))
class DownloadSelectedFeedbackController extends CourseworkController {
	var feedbackDao = Wire.auto[FeedbackDao]
	var fileServer = Wire.auto[FileServer]
	
	@ModelAttribute def singleFeedbackCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment, @PathVariable("feedbackId") feedbackId: String) = 
		new AdminGetSingleFeedbackCommand(module, assignment, mandatory(feedbackDao.getFeedback(feedbackId)))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(cmd: AdminGetSingleFeedbackCommand, @PathVariable("filename") filename: String)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		fileServer.serve(cmd.apply())
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedbacks.zip"))
class DownloadAllFeedbackController extends CourseworkController {
	
	var fileServer = Wire.auto[FileServer]
	
	@ModelAttribute def selectedFeedbacksCommand(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new DownloadSelectedFeedbackCommand(module, assignment)
	
	@RequestMapping
    def getSelected(command: DownloadSelectedFeedbackCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
        val (assignment, module, filename) = (command.assignment, command.module, command.filename)
        command.apply { renderable =>
            fileServer.serve(renderable)
        }
    }   
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/download-zip/{filename}"))
class DownloadAllFeedback extends CourseworkController {
	var fileServer = Wire.auto[FileServer]
	
	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new AdminGetAllFeedbackCommand(module, assignment)
	
	@RequestMapping
	def download(cmd: AdminGetAllFeedbackCommand, @PathVariable("filename") filename: String)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		fileServer.serve(cmd.apply())
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/list"))
class ListFeedback extends CourseworkController {	
	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new ListFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def get(cmd: ListFeedbackCommand) = {
		Mav("admin/assignments/feedback/list",
			"whoDownloaded" -> cmd.apply())
			.crumbs(Breadcrumbs.Department(cmd.module.department), Breadcrumbs.Module(cmd.module))
	}
}

