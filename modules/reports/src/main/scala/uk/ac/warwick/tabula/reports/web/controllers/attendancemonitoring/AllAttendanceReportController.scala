package uk.ac.warwick.tabula.reports.web.controllers.attendancemonitoring

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.reports.commands.attendancemonitoring._


@Controller
@RequestMapping(Array("/{department}/{academicYear}/attendance/all"))
class AllAttendanceReportController extends AbstractAttendanceReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllAttendanceReportCommand(mandatory(department), mandatory(academicYear), AttendanceReportFilters.identity)

	val pageRenderPath = "allattendance"
	val filePrefix = "all-monitoring-point-attendance"

}

@Controller
@RequestMapping(Array("/{department}/{academicYear}/attendance/unrecorded"))
class UnrecordedAttendanceReportController extends AbstractAttendanceReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllAttendanceReportCommand(mandatory(department), mandatory(academicYear), AttendanceReportFilters.unrecorded)

	val pageRenderPath = "unrecorded"
	val filePrefix = "unrecorded-monitoring-points"

}

@Controller
@RequestMapping(Array("/{department}/{academicYear}/attendance/missed"))
class MissedAttendanceReportController extends AbstractAttendanceReportController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department, @PathVariable("academicYear") academicYear: AcademicYear) =
		AllAttendanceReportCommand(mandatory(department), mandatory(academicYear), AttendanceReportFilters.missedUnauthorised)

	val pageRenderPath = "missed"
	val filePrefix = "missed-monitoring-points"

}