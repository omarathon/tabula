package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.AcademicYear
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.commands.manage.{SetsFindPointsResultOnCommandState, DeleteAttendancePointCommand, FindPointsCommand, FindPointsResult}
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/editpoints/{templatePoint}/delete"))
class DeleteAttendancePointController extends AttendanceController {

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(department, academicYear, None)

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	) = {
		DeleteAttendancePointCommand(mandatory(department), mandatory(templatePoint))
	}

	private def render(department: Department, academicYear: AcademicYear) = {
		Mav("manage/deletepoint",
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear),
			Breadcrumbs.Manage.EditPoints(department, academicYear)
		)
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with SetsFindPointsResultOnCommandState,
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val findCommandResult = findCommand.apply()
		cmd.setFindPointsResult(findCommandResult)
		render(department, academicYear)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]]
			with SetsFindPointsResultOnCommandState with SelfValidating,
		errors: Errors,
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val findCommandResult = findCommand.apply()
		cmd.setFindPointsResult(findCommandResult)
		cmd.validate(errors)
		if (errors.hasErrors) {
			render(department, academicYear)
		} else {
			val points = cmd.apply()
			Redirect(
				getReturnTo(Routes.Manage.editPoints(department, academicYear)),
				"points" -> points.size.toString,
				"actionCompleted" -> "deleted"
			)
		}
	}

}
