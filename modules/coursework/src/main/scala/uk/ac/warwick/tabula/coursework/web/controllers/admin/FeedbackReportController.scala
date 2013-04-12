package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.commands.departments.FeedbackReportCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.services.feedbackreport.FeedbackReport
import uk.ac.warwick.tabula.web.views.ExcelView
import uk.ac.warwick.tabula.coursework.web.Routes
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService

@Controller
@RequestMapping(Array("/admin/department/{dept}/reports/feedback"))
class FeedbackReportController extends CourseworkController {

	@Autowired var jobService: JobService = _

	@ModelAttribute def command(@PathVariable(value = "dept") dept: Department, user: CurrentUser) =
		new FeedbackReportCommand(dept, user)

	@RequestMapping(method=Array(HEAD, GET), params = Array("!jobId"))
	def requestReport(cmd:FeedbackReportCommand, errors:Errors):Mav = {
		val dateFormat = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss")
		val model = Mav("admin/assignments/feedbackreport/report_range",
			"department" -> cmd.department,
			"startDate" -> dateFormat.print(DateTime.now.minusMonths(3)),
			"endDate" -> dateFormat.print(DateTime.now)
		).noLayout()
		model
	}


	@RequestMapping(method = Array(POST), params = Array("!jobId"))
	def generateReport(cmd: FeedbackReportCommand) = {
		val jobId = cmd.apply().id
		Redirect(Routes.admin.feedbackReports(cmd.department) + "?jobId=" + jobId)
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String) = {
		val job = jobService.getInstance(jobId)
		val mav = Mav("admin/assignments/feedbackreport/progress", "job" -> job).noLayoutIf(ajax)
		mav
	}


}