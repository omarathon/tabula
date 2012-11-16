package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.hibernate.annotations._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.persistence._
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.services.jobs.JobService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.AssignmentService

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/turnitin"))
class TurnitinController extends CourseworkController {

	@Autowired var jobService: JobService = _
	@Autowired var assignmentService: AssignmentService = _

	@ModelAttribute def model(user: CurrentUser) = new SubmitToTurnitinCommand(user)

	@RequestMapping(method = Array(GET, HEAD), params = Array("!jobId"))
	def confirm(command: SubmitToTurnitinCommand) = {
		check(command)
		Mav("admin/assignments/turnitin/form", "incompatibleFiles" -> command.incompatibleFiles)
	}

	@RequestMapping(method = Array(POST), params = Array("!jobId"))
	def submit(command: SubmitToTurnitinCommand) = {
		check(command)
		val jobId = command.apply()
		Redirect(Routes.admin.assignment.turnitin.status(command.assignment) + "?jobId=" + jobId)
	}

	@RequestMapping(params = Array("jobId"))
	def status(@RequestParam jobId: String) = {
		val job = jobService.getInstance(jobId)
		val mav = Mav("admin/assignments/turnitin/status", "job" -> job).noLayoutIf(ajax)
		// add assignment object if we can find it. FIXME This is a bit hacky.
		job foreach { job =>
			assignmentService.getAssignmentById(job.getString("assignment")) foreach { assignment =>
				mav.addObjects("assignment" -> assignment)
			}
		}
		mav
	}

	def check(command: SubmitToTurnitinCommand) {
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
	}

}