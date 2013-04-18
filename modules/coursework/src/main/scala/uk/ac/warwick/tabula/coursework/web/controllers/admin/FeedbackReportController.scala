package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.{DateFormats, CurrentUser}
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.commands.departments.FeedbackReportCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.services.jobs.JobService
import org.hibernate.validator.Valid
import uk.ac.warwick.spring.Wire


@Controller
@RequestMapping(Array("/admin/department/{dept}/reports/feedback"))
class FeedbackReportController extends CourseworkController {

	validatesSelf[FeedbackReportCommand]

	var jobService = Wire[JobService]
	var context = Wire[String]("${module.context}")

	@ModelAttribute def command(@PathVariable(value = "dept") dept: Department, user: CurrentUser) =
		new FeedbackReportCommand(dept, user)

	@RequestMapping(method=Array(HEAD, GET), params = Array("!jobId"))
	def requestReport(cmd:FeedbackReportCommand, errors:Errors):Mav = {
		val formatter = DateTimeFormat.forPattern(DateFormats.DateTimePicker)
		val model = Mav("admin/assignments/feedbackreport/report_range",
			"department" -> cmd.department,
			"startDate" ->  formatter.print(new DateTime().minusMonths(3)),
			"endDate" ->  formatter.print(new DateTime())
		).noLayout()
		model
	}


	@RequestMapping(method = Array(POST), params = Array("!jobId"))
	def generateReport(@Valid cmd: FeedbackReportCommand, errors: Errors) = {
		if(errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val jobId = cmd.apply().id
			val successUrl = context + Routes.admin.feedbackReports(cmd.department) + "?jobId=" + jobId
			Mav(new JSONView(Map("status" -> "success", "result" -> successUrl)))
		}
	}

	@RequestMapping(params = Array("jobId"))
	def checkProgress(@RequestParam jobId: String) = {
		val job = jobService.getInstance(jobId)
		val mav = Mav("admin/assignments/feedbackreport/progress", "job" -> job).noLayoutIf(ajax)
		mav
	}


}