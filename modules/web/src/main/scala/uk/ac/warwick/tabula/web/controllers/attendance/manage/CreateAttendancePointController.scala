package uk.ac.warwick.tabula.web.controllers.attendance.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.data.model.{Department, MeetingFormat}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.attendance.manage.CreateAttendancePointCommand
import javax.validation.Valid

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.Mav

import collection.JavaConverters._

@Controller
@RequestMapping(Array("/attendance/manage/{department}/{academicYear}/addpoints/new"))
class CreateAttendancePointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam schemes: JList[AttendanceMonitoringScheme]
	) =
		CreateAttendancePointCommand(mandatory(department), mandatory(academicYear), schemes.asScala.toSeq)

	@RequestMapping(method = Array(POST))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav("attendance/manage/newpoint",
			"allMeetingFormats" -> MeetingFormat.members,
			"returnTo" -> getReturnTo("")
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("submit"))
	def submitNormal(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			form(cmd, department, academicYear)
		} else {
			doApply(cmd)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("submitConfirm"))
	def submitSkipOverlap(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors && errors.getErrorCount != 1 && errors.getAllErrors.get(0).getCode != "attendanceMonitoringPoint.overlaps") {
			form(cmd, department, academicYear)
		} else {
			doApply(cmd)
		}
	}

	private def doApply(cmd: Appliable[Seq[AttendanceMonitoringPoint]]) = {
		val points = cmd.apply()
		Redirect(
			getReturnTo(""),
			"points" -> points.size.toString,
			"schemes" -> points.map(_.scheme.id).mkString(",")
		)
	}

	@RequestMapping(method = Array(POST), params = Array("cancel"))
	def cancel(@RequestParam schemes: JList[AttendanceMonitoringScheme]): Mav = {
		Redirect(
			getReturnTo(""),
			"schemes" -> schemes.asScala.map(_.id).mkString(",")
		)
	}

}
