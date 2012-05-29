package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.commands.assignments.ListSubmissionsCommand
import uk.ac.warwick.courses.web.Mav
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.actions.Participate
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.commands.assignments.DownloadAllSubmissionsCommand
import org.springframework.beans.factory.annotation.Autowired
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.commands.assignments.DownloadSubmissionsCommand

@Configurable @Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/submissions/list"))
class ListSubmissions extends BaseController {

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def list(command:ListSubmissionsCommand) = {
		val (assignment, module) = (command.assignment, command.module)
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
		
		val submissions = command.apply()
		
		Mav("admin/assignments/submissions/list",
				"assignment" -> assignment,
				"submissions" -> submissions)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
	
}

@Configurable @Controller
@RequestMapping
class DownloadSubmissionsController extends BaseController {

	@Autowired var fileServer:FileServer =_
	
	@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/submissions.zip"))
	def download(command:DownloadSubmissionsCommand, response:HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable => 
			fileServer.serve(renderable, response)
		}
	}
	
	@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
	def downloadAll(command:DownloadAllSubmissionsCommand, response:HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable => 
			fileServer.serve(renderable, response)
		}
	}
	
}