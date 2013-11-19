package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.attendance.commands.{StudentRecordCommandState, StudentRecordCommand}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/view/{department}/students/{student}/record"))
class StudentRecordController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable student: StudentMember,
		user: CurrentUser,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear
	) = {
		StudentRecordCommand(department, student, user, Option(academicYear))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") cmd: StudentRecordCommand) = {
		cmd.populate()
		form(cmd)
	}

	def form(cmd: Appliable[Seq[MonitoringCheckpoint]] with StudentRecordCommandState) = {
		Mav("home/record_student",
			"returnTo" -> getReturnTo(Routes.department.viewStudents(cmd.department))
		).crumbs(Breadcrumbs.ViewDepartment(cmd.department), Breadcrumbs.ViewDepartmentStudents(cmd.department))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]] with StudentRecordCommandState, errors: Errors) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.department.viewStudents(cmd.department))
		}
	}

}
