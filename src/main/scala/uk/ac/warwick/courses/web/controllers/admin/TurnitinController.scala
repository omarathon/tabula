package uk.ac.warwick.courses.web.controllers.admin

import org.hibernate.annotations._
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import javax.persistence._
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.commands.turnitin.SubmitToTurnitinCommand
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.services.jobs.JobService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.CurrentUser

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/turnitin"))
class TurnitinController extends BaseController {
	
	@Autowired var jobService: JobService =_
	
	@ModelAttribute def model(user: CurrentUser) = new SubmitToTurnitinCommand(user)
	
	@RequestMapping(method=Array(GET,HEAD), params=Array("!jobId"))
	def confirm(command: SubmitToTurnitinCommand) = {
		check(command)
		Mav("admin/assignments/turnitin/form")
	}
	
	@RequestMapping(method=Array(POST))
	def submit(command: SubmitToTurnitinCommand) = {
		check(command)
		val jobId = command.apply()
		Redirect(Routes.admin.assignment.turnitin.status(command.assignment)+"?jobId="+jobId)
	}
	
	@RequestMapping(method=Array(GET,HEAD), params=Array("jobId"))
	def status(@RequestParam jobId: String) {
		val job = jobService.getInstance(jobId)
		Mav("admin/assignments/turnitin/status", "job" -> job)
	}
	
	def check(command: SubmitToTurnitinCommand) {
		mustBeLinked(mandatory(command.assignment), mandatory(command.module))
		mustBeAbleTo(Participate(command.module))
	}
	
}