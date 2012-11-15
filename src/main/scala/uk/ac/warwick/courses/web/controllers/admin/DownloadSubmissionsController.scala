package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.commands.assignments.DownloadAllSubmissionsCommand
import uk.ac.warwick.courses.commands.assignments.DownloadSubmissionsCommand
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.services.AssignmentService
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses._
import uk.ac.warwick.courses.commands.assignments.AdminGetSingleSubmissionCommand

@Configurable @Controller
@RequestMapping
class DownloadSubmissionsController extends BaseController {

	@Autowired var fileServer: FileServer = _
    @Autowired var assignmentService: AssignmentService = _
        
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
	
	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download/{submissionId}/{filename}"))
    def downloadSingle(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable submissionId: String, @PathVariable filename: String, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
	    mustBeAbleTo(Participate(module))

	    assignmentService.getSubmission(submissionId) match {
	        case Some(submission) => {
	            mustBeLinked(submission, assignment)
                val renderable = new AdminGetSingleSubmissionCommand(submission).apply()
                fileServer.serve(renderable, response)
	        }
	        case None => throw new ItemNotFoundException
        }
    }
}
