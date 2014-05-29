package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.AcademicYear
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.commands.manage.{DeleteAttendancePointCommand, FindPointsCommand, FindPointsResult}
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/editpoints/{templatePoint}/delete"))
class DeleteAttendancePointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(department, academicYear, None)

	@ModelAttribute("command")
	def command(
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable templatePoint: AttendanceMonitoringPoint
	) = {
		DeleteAttendancePointCommand(department, templatePoint, findCommand.apply())
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
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] ,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		render(department, academicYear)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		if (errors.hasErrors) {
			render(department, academicYear)
		} else {
			val points = cmd.apply()
			Redirect(
				getReturnTo(Routes.Manage.editPoints(department, academicYear)),
				"points" -> points.size.toString
			)
		}
	}

}
