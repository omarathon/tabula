package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.attendance.commands.StudentRecordCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.commands.StudentRecordCommandState
import uk.ac.warwick.tabula.commands.PopulateOnForm

@Controller
@RequestMapping(Array("/view/{department}/2013/students/{student}/record"))
class StudentRecordController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable student: StudentMember,
		user: CurrentUser
	): Appliable[Seq[MonitoringCheckpoint]] with PopulateOnForm with StudentRecordCommandState
		= StudentRecordCommand(department, student, user, Option(AcademicYear(2013)))

	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]] with StudentRecordCommandState with PopulateOnForm) = {
		cmd.populate()
		form(cmd)
	}

	def form(cmd: Appliable[Seq[MonitoringCheckpoint]] with StudentRecordCommandState) = {
		Mav("home/record_student",
			"returnTo" -> getReturnTo(Routes.old.department.viewStudents(cmd.department))
		).crumbs(Breadcrumbs.Old.ViewDepartment(cmd.department), Breadcrumbs.Old.ViewDepartmentStudents(cmd.department))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]] with StudentRecordCommandState, errors: Errors) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.old.department.viewStudents(cmd.department))
		}
	}

}
