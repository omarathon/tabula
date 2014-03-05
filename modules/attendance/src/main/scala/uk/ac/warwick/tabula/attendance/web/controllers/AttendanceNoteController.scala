package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{AbsenceType, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointAttendanceNote, MonitoringPoint}
import uk.ac.warwick.tabula.commands.{SelfValidating, ApplyWithCallback, PopulateOnForm, Appliable}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.commands.{CheckpointUpdatedDescription, AttendanceNoteAttachmentCommand, EditAttendanceNoteCommand}
import uk.ac.warwick.tabula.attendance.web.Routes
import scala.Array
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.fileserver.{FileServer, RenderableFile}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.{UserLookupService, ProfileService, MonitoringPointService}
import uk.ac.warwick.tabula.helpers.DateBuilder
import javax.validation.Valid

@Controller
@RequestMapping(Array("/note/{student}/{monitoringPoint}"))
class AttendanceNoteController extends AttendanceController with CheckpointUpdatedDescription {

	@Autowired var monitoringPointService: MonitoringPointService = _
	@Autowired var userLookup: UserLookupService = _

	@RequestMapping
	def home(
		@PathVariable student: StudentMember,
		@PathVariable monitoringPoint: MonitoringPoint
	) = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse(throw new ItemNotFoundException())
		val checkpoint = monitoringPointService.getCheckpoint(student, monitoringPoint).getOrElse(null)
		Mav("home/view_note",
			"attendanceNote" -> attendanceNote,
			"checkpoint" -> checkpoint,
			"checkpointDescription" -> Option(checkpoint).map{ checkpoint => describeCheckpoint(checkpoint)}.getOrElse(""),
			"updatedBy" -> userLookup.getUserByUserId(attendanceNote.updatedBy).getFullName,
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		).noLayoutIf(ajax)
	}

}

@Controller
@RequestMapping(Array("/note/{student}/{monitoringPoint}/attachment/{fileName}"))
class AttendanceNoteAttachmentController extends AttendanceController {

	@Autowired var fileServer: FileServer = _

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable monitoringPoint: MonitoringPoint) =
		AttendanceNoteAttachmentCommand(student, monitoringPoint, user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: ApplyWithCallback[Option[RenderableFile]])
		(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		cmd.callback = { (renderable) => renderable.map{fileServer.serve} }
		cmd.apply().orElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(Array("/note/{student}/{monitoringPoint}/edit"))
class EditAttendanceNoteController extends AttendanceController {

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
		val mav = Mav("home/edit_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"returnTo" -> getReturnTo(Routes.department.viewStudent(currentMember.homeDepartment, student)),
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
			form(cmd, student, true)
		} else {
			cmd.apply()
			Mav("home/edit_note", "success" -> true, "isIframe" -> true, "allAbsenceTypes" -> AbsenceType.values)
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
			Redirect(Routes.department.viewStudent(currentMember.homeDepartment, student))
		}
	}

}
