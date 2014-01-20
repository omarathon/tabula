package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointAttendanceNote, MonitoringPoint}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.commands.AttendanceNoteCommand
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/note/{student}/{monitoringPoint}"))
class AttendanceNoteController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable monitoringPoint: MonitoringPoint) =
		AttendanceNoteCommand(student, monitoringPoint, user)

	@RequestMapping(method=Array(GET, HEAD), params=Array("isModal"))
	def getModal(
		@ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		@PathVariable student: StudentMember
	) = {
		cmd.populate()
		form(cmd, student, isModal = true)
	}

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		@PathVariable student: StudentMember
	) = {
		cmd.populate()
		form(cmd, student, isIframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		@PathVariable student: StudentMember
	) = {
		cmd.populate()
		form(cmd, student)
	}

	private def form(
		cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		student: StudentMember,
		isModal: Boolean = false,
		isIframe: Boolean = false
	) = {
		Mav("home/note",
			"returnTo" -> getReturnTo(Routes.department.viewStudent(currentMember.homeDepartment, student)),
			"isModal" -> isModal,
			"isIframe" -> isIframe
		).noLayoutIf(isModal || isIframe)
	}

	@RequestMapping(method=Array(POST), params=Array("isIframe"))
	def submitIframe(
		@ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember
	) = {
		if (errors.hasErrors) {
			form(cmd, student)
		} else {
			cmd.apply()
			Mav("home/note", "success" -> true)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember
	) = {
		if (errors.hasErrors) {
			form(cmd, student)
		} else {
			cmd.apply()
			Redirect(Routes.department.viewStudent(currentMember.homeDepartment, student))
		}
	}

}
