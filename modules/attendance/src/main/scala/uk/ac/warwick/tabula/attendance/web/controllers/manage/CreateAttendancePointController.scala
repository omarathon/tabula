package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{MeetingFormat, Department}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringScheme, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.attendance.commands.manage.CreateAttendancePointCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints/new"))
class CreateAttendancePointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
			CreateAttendancePointCommand(department, academicYear)

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		Mav("manage/newpoint",
			"allMeetingFormats" -> MeetingFormat.members,
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("submit"))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		if (errors.hasErrors) {
			form(cmd, department, academicYear)
		} else {
			val points = cmd.apply()
			Redirect(
				getReturnTo(""),
				"points" -> points.size.toString,
				"schemes" -> points.map(_.scheme.id).mkString(",")
			)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) = {
		Redirect(
			getReturnTo(""),
			"schemes" -> schemes.asScala.map(_.id).mkString(",")
		)
	}

}
