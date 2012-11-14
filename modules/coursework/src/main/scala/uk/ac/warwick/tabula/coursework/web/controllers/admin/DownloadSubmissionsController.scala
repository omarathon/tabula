package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.coursework.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.assignments.{DownloadFeedbackSheetsCommand, DownloadAllSubmissionsCommand, DownloadSubmissionsCommand}
import uk.ac.warwick.tabula.coursework.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.spring.Wire

@Controller
class DownloadSubmissionsController extends BaseController {

	var fileServer = Wire.auto[FileServer]

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.zip"))
	def download(command: DownloadSubmissionsCommand, response: HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable =>
			fileServer.serve(renderable, response)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
	def downloadAll(command: DownloadAllSubmissionsCommand, response: HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable =>
			fileServer.serve(renderable, response)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback-templates.zip"))
	def downloadFeedbackTemplatesOnly(command: DownloadFeedbackSheetsCommand, response: HttpServletResponse) {
		val assignment = command.assignment
		mustBeLinked(assignment, assignment.module)
		mustBeAbleTo(Participate(assignment.module))
		command.apply { renderable =>
			fileServer.serve(renderable, response)
		}
	}

}