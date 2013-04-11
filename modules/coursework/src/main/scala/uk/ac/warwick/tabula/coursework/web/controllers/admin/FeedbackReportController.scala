package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import scala.Array
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.commands.departments.FeedbackReportCommand
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import uk.ac.warwick.tabula.coursework.services.feedbackreport.FeedbackReport
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(Array("/admin/department/{dept}/reports/feedback"))
class FeedbackReportController extends CourseworkController {
	
	@ModelAttribute def command(@PathVariable(value = "dept") dept: Department, user: CurrentUser) =
		new FeedbackReportCommand(dept, user)

	@RequestMapping(method=Array(HEAD, GET))
	def requestReport(cmd:FeedbackReportCommand, errors:Errors):Mav = {
		val dateFormat = DateTimeFormat.forPattern("dd-MMM-yyyy HH:mm:ss")
		val model = Mav("admin/assignments/feedbackreport/report_range",
			"department" -> cmd.department,
			"startDate" -> dateFormat.print(DateTime.now.minusMonths(3)),
			"endDate" -> dateFormat.print(DateTime.now)
		).noLayout()
		model
	}

	//TODO-RITCHIE Take this out before merging back to develop
	@RequestMapping(method = Array(POST), params = Array("test"))
	def generateTestReport(cmd: FeedbackReportCommand) = {
		val report = new FeedbackReport(cmd.department, cmd.startDate, cmd.endDate)

		val assignmentSheet = report.generateAssignmentSheet(cmd.department)
		val moduleSheet = report.generateModuleSheet(cmd.department)

		report.buildAssignmentData()

		report.populateAssignmentSheet(assignmentSheet)
		report.formatWorksheet(assignmentSheet, 11)
		report.populateModuleSheet(moduleSheet)

		report.formatWorksheet(assignmentSheet, 11)
		report.formatWorksheet(moduleSheet, 11)

		new ExcelView(cmd.department.getName + " feedback report.xlsx", report.workbook)
	}

	@RequestMapping(method = Array(POST), params = Array("!test"))
	def generateReport(cmd: FeedbackReportCommand) = {
		val jobId = cmd.apply().id
		Mav("admin/assignments/feedbackreport/progress")
	}

}