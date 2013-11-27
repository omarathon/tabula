package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.attendance.commands.report.{ReportStudentsState, ReportStudentsConfirmCommand, ReportStudentsChoosePeriodCommand}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/report/{department}"))
class ReportStudentsChoosePeriodController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @RequestParam academicYear: AcademicYear) =
		ReportStudentsChoosePeriodCommand(department, mandatory(academicYear))

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[(StudentMember, Int)]]) = {
		Mav("report/periods")
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[(StudentMember, Int)]], errors: Errors) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			Mav("report/students", "students" -> cmd.apply())
		}
	}

}

@Controller
@RequestMapping(Array("/report/{department}/confirm"))
class ReportStudentsConfirmController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department) =
		ReportStudentsConfirmCommand(department, user)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointReport]]) = {
		Mav("report/confirm")
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointReport]] with ReportStudentsState, errors: Errors, @PathVariable department: Department) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			val reports = cmd.apply()
			Redirect(Routes.department.viewStudents(department), "reports" -> reports.size, "monitoringPeriod" -> cmd.period)
		}
	}

}
