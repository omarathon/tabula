package uk.ac.warwick.tabula.attendance.web.controllers.view.old

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.report.OldReportStudentsChoosePeriodCommand.StudentReportStatus
import uk.ac.warwick.tabula.attendance.commands.report.{OldReportStudentsChoosePeriodCommand, ReportStudentsConfirmCommand, ReportStudentsState}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport

@Controller
@RequestMapping(Array("/report/{department}"))
class OldReportStudentsChoosePeriodController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department) =
		OldReportStudentsChoosePeriodCommand(department, mandatory(AcademicYear(2013)))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[StudentReportStatus]]) = {
		Mav("report/periods")
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentReportStatus]], errors: Errors) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			val studentReportStatuses = cmd.apply()
			Mav("report/students", "studentReportStatuses" -> studentReportStatuses, "unrecordedStudentsCount" -> studentReportStatuses.count(_.unrecorded > 0))
		}
	}

}

@Controller
@RequestMapping(Array("/report/{department}/confirm"))
class OldReportStudentsConfirmController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department) =
		ReportStudentsConfirmCommand(department, user)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointReport]]) = {
		Mav("report/confirm")
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointReport]] with ReportStudentsState,
		errors: Errors,
		@PathVariable department: Department
	) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			val reports = cmd.apply()
			val redirectObjects = Map("reports" -> reports.size, "monitoringPeriod" -> cmd.period, "academicYear" -> cmd.academicYear) ++ cmd.filterMap
			Redirect(Routes.old.department.viewStudents(department), redirectObjects)
		}
	}

}
