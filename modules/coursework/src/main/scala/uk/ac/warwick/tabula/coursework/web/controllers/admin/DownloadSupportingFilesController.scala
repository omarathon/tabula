package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.fileserver.FileServer
import scala.Array
import uk.ac.warwick.tabula.coursework.commands.assignments.{DownloadSupportingFilesCommand, DownloadAttachmentCommand}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import javax.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.coursework.commands.assignments.AdminDownloadSupportingFilesCommand

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}/extension"))
class DownloadSupportingFilesController extends CourseworkController{

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable filename: String, user:CurrentUser) = {
		new DownloadSupportingFilesCommand(module, assignment, mandatory(assignment.findExtension(user.universityId)), filename)
	}

	@Autowired var fileServer:FileServer =_

	@RequestMapping(value=Array("/supporting-file/{filename}"), method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command:DownloadSupportingFilesCommand, user:CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse) = {
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable)	}
		command.apply().orElse{ throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/extensions/review-request/{universityId}"))
class AdminDownloadSupportingFilesController extends CourseworkController{

	@ModelAttribute def command(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable filename: String, @PathVariable universityId: String) = {
		new AdminDownloadSupportingFilesCommand(module, assignment, mandatory(assignment.findExtension(universityId)), filename)
	}

	@Autowired var fileServer:FileServer =_

	@RequestMapping(value=Array("/supporting-file/{filename}"), method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def getAttachment(command:AdminDownloadSupportingFilesCommand, user:CurrentUser)(implicit request: HttpServletRequest, response: HttpServletResponse) = {
		// specify callback so that audit logging happens around file serving
		command.callback = { (renderable) => fileServer.serve(renderable)	}
		command.apply().orElse{ throw new ItemNotFoundException() }
	}

}