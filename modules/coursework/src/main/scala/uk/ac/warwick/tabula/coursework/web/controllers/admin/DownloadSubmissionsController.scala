package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.actions.{DownloadSubmissions, Participate}
import uk.ac.warwick.tabula.coursework.commands.assignments.{DownloadFeedbackSheetsCommand, DownloadAllSubmissionsCommand, DownloadSubmissionsCommand}
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{UserLookupService, AssignmentService}
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.{MarkingCompleted, Module, Assignment}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.coursework.commands.assignments.AdminGetSingleSubmissionCommand
import javax.servlet.http.HttpServletRequest

@Controller
class DownloadSubmissionsController extends CourseworkController {

	var fileServer = Wire.auto[FileServer]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]


	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions.zip"))
	def download(command: DownloadSubmissionsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/submissions.zip"))
	def downloadMarkersSubmissions(command: DownloadSubmissionsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val (assignment, module) = (command.assignment, command.module)
		mustBeLinked(assignment, module)
		mustBeAbleTo(DownloadSubmissions(assignment))

		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		// do not download submissions where the marker has completed marking
		val filteredSubmissions = submissions.filter{ submission =>
			val markerFeedback =  assignment.getMarkerFeedback(submission.universityId, user.apparentUser)
			markerFeedback match {
				case Some(f) if f.state != MarkingCompleted => true
				case _ => false
			}
		}

		command.submissions = filteredSubmissions.toList

		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download-zip/{filename}"))
	def downloadAll(command: DownloadAllSubmissionsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val (assignment, module, filename) = (command.assignment, command.module, command.filename)
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/download/{submissionId}/{filename}"))
    def downloadSingle(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable submissionId: String, @PathVariable filename: String)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		mustBeLinked(assignment, module)
	    mustBeAbleTo(Participate(module))

	    assignmentService.getSubmission(submissionId) match {
	        case Some(submission) => {
	            mustBeLinked(submission, assignment)
                val renderable = new AdminGetSingleSubmissionCommand(submission).apply()
                fileServer.serve(renderable)
	        }
	        case None => throw new ItemNotFoundException
        }
    }

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback-templates.zip"))
	def downloadFeedbackTemplatesOnly(command: DownloadFeedbackSheetsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val assignment = command.assignment
		mustBeLinked(assignment, assignment.module)
		mustBeAbleTo(Participate(assignment.module))
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

	@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker-templates.zip"))
	def downloadMarkerFeedbackTemplates(command: DownloadFeedbackSheetsCommand)(implicit request: HttpServletRequest, response: HttpServletResponse) {
		val assignment = command.assignment
		mustBeLinked(assignment, assignment.module)
		mustBeAbleTo(Participate(assignment.module))

		val submissions = assignment.getMarkersSubmissions(user.apparentUser)

		val users = submissions.map(s => userLookup.getUserByUserId(s.userId))
		command.members = users
		command.apply { renderable =>
			fileServer.serve(renderable)
		}
	}

}
