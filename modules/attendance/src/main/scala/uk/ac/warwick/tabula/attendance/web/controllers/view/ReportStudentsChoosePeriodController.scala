package uk.ac.warwick.tabula.attendance.web.controllers.view

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.view.{ReportStudentsChoosePeriodCommand, StudentReportCount}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(Array("/view/{department}/{academicYear}/report"))
class ReportStudentsChoosePeriodController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		ReportStudentsChoosePeriodCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[StudentReportCount]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		Mav("view/reportperiod")
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentReportCount]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		if(errors.hasErrors) {
			form(cmd, department, academicYear)
		} else {
			val studentReportCounts = cmd.apply()
			Mav("view/reportstudents",
				"studentReportCounts" -> studentReportCounts,
				"unrecordedStudentsCount" -> studentReportCounts.count(_.unrecorded > 0)
			)
		}
	}

}
