package uk.ac.warwick.tabula.web.controllers.attendance.old

import javax.validation.Valid

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.note.old.{AttendanceNoteAttachmentCommand, EditAttendanceNoteCommand}
import uk.ac.warwick.tabula.commands.attendance.old.CheckpointUpdatedDescription
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointAttendanceNote}
import uk.ac.warwick.tabula.data.model.{AbsenceType, StudentMember}
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.{MonitoringPointService, UserLookupService}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/note/2013/{student}/{monitoringPoint}"))
class OldAttendanceNoteController extends AttendanceController with CheckpointUpdatedDescription {

	@Autowired var monitoringPointService: MonitoringPointService = _
	@Autowired var userLookup: UserLookupService = _

	@RequestMapping
	def home(
		@PathVariable student: StudentMember,
		@PathVariable monitoringPoint: MonitoringPoint
	) = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse(throw new ItemNotFoundException())
		val checkpoint = monitoringPointService.getCheckpoint(student, monitoringPoint).getOrElse(null)
		Mav("attendance/note/old/view_note",
			"attendanceNote" -> attendanceNote,
			"checkpoint" -> checkpoint,
			"checkpointDescription" -> Option(checkpoint).map{ checkpoint => describeCheckpoint(checkpoint)}.getOrElse(""),
			"updatedBy" -> userLookup.getUserByUserId(attendanceNote.updatedBy).getFullName,
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		).noLayoutIf(ajax)
	}

}

@RequestMapping(Array("/attendance/note/2013/{student}/{monitoringPoint}/attachment/{fileName}"))
class OldAttendanceNoteAttachmentController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable monitoringPoint: MonitoringPoint): Appliable[Option[RenderableFile]] =
		AttendanceNoteAttachmentCommand(mandatory(student), mandatory(monitoringPoint), user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: Appliable[Option[RenderableFile]]): RenderableFile = {
		cmd.apply().getOrElse { throw new ItemNotFoundException() }
	}

}

@RequestMapping(Array("/attendance/note/2013/{student}/{monitoringPoint}/edit"))
class OldEditAttendanceNoteController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable student: StudentMember,
		@PathVariable monitoringPoint: MonitoringPoint,
		@RequestParam(value="state", required=false) state: String
	) =
		EditAttendanceNoteCommand(student, monitoringPoint, user, Option(state))

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
		isIframe: Boolean = false
	) = {
		val mav = Mav("attendance/note/old/edit_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"returnTo" -> getReturnTo(Routes.old.department.viewStudent(currentMember.homeDepartment, student)),
			"isModal" -> ajax,
			"isIframe" -> isIframe
		)
		if(ajax)
			mav.noLayout()
		else if (isIframe)
			mav.noNavigation()
		else
			mav
	}

	@RequestMapping(method=Array(POST), params=Array("isIframe"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember
	) = {
		if (errors.hasErrors) {
			form(cmd, student, isIframe = true)
		} else {
			cmd.apply()
			Mav("attendance/note/old/edit_note", "success" -> true, "isIframe" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointAttendanceNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember
	) = {
		if (errors.hasErrors) {
			form(cmd, student)
		} else {
			cmd.apply()
			Redirect(Routes.old.department.viewStudent(currentMember.homeDepartment, student))
		}
	}

}
