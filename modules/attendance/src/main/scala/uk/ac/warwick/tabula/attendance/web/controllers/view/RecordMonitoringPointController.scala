package uk.ac.warwick.tabula.attendance.web.controllers.view

import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.attendance.web.controllers.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.attendance.commands.GroupedPoint
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.view.{SetFilteredPointsOnRecordMonitoringPointCommand, RecordMonitoringPointCommand, FilterMonitoringPointsCommand}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(value=Array("/view/{department}/{academicYear}/points/{templatePoint}/record"))
class RecordMonitoringPointController extends AttendanceController with HasMonthNames {

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable templatePoint: AttendanceMonitoringPoint) =
		RecordMonitoringPointCommand(mandatory(department), mandatory(academicYear), mandatory(templatePoint), user)

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("filterCommand") filterCommand: Appliable[Map[String, Seq[GroupedPoint]]],
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]]
			with PopulateOnForm with SetFilteredPointsOnRecordMonitoringPointCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val filterResult = filterCommand.apply()
		cmd.setFilteredPoints(filterResult)
		cmd.populate()
		render(department, academicYear)
	}

	private def render(department: Department, academicYear: AcademicYear) = {
		Mav("view/pointrecord",
			"returnTo" -> getReturnTo(Routes.View.points(department, academicYear))
		).crumbs(
			Breadcrumbs.View.Home,
			Breadcrumbs.View.Department(department),
			Breadcrumbs.View.DepartmentForYear(department, academicYear),
			Breadcrumbs.View.Points(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("filterCommand") filterCommand: Appliable[Map[String, Seq[GroupedPoint]]],
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringCheckpoint]]
			with SetFilteredPointsOnRecordMonitoringPointCommand with SelfValidating,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val filterResult = filterCommand.apply()
		cmd.setFilteredPoints(filterResult)
		cmd.validate(errors)
		if (errors.hasErrors) {
			render(department, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.View.points(department, academicYear))
		}
	}

}