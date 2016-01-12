package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, AbsenceType}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.commands.{ApplyWithCallback, PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.attendance.web.Routes
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.commands.note.{BulkAttendanceNoteCommand, AttendanceNoteAttachmentCommand, EditAttendanceNoteCommand}
import uk.ac.warwick.tabula.services.fileserver.{RenderableFile, FileServer}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

@Controller
@RequestMapping(Array("/note/{academicYear}/{student}/{point}"))
class AttendanceNoteController extends AttendanceController {

	@Autowired var monitoringPointService: AttendanceMonitoringService = _
	@Autowired var userLookup: UserLookupService = _

	@RequestMapping
	def home(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@PathVariable academicYear: AcademicYear
	) = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, point).getOrElse(throw new ItemNotFoundException())

		val mav = Mav("note/view_note",
			"attendanceNote" -> attendanceNote,
			"academicYear" -> academicYear.startYear.toString,
			"updatedBy" -> userLookup.getUserByUserId(attendanceNote.updatedBy).getFullName,
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		)

		val pointsCheckpointsMap = monitoringPointService.getCheckpoints(Seq(point), student)
		if (pointsCheckpointsMap.nonEmpty) mav.addObjects("checkpoint" -> pointsCheckpointsMap.head._2)

		mav.noLayoutIf(ajax)

	}

}

@Controller
@RequestMapping(Array("/note/{academicYear}/{student}/{point}/attachment/{fileName}"))
class AttendanceNoteAttachmentController extends AttendanceController {

	@Autowired var fileServer: FileServer = _

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable point: AttendanceMonitoringPoint) =
		AttendanceNoteAttachmentCommand(mandatory(student), mandatory(point), user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: ApplyWithCallback[Option[RenderableFile]])
		(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		cmd.callback = { (renderable) => renderable.foreach { fileServer.serve(_) } }
		cmd.apply().orElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(Array("/note/{academicYear}/{student}/{point}/edit"))
class EditAttendanceNoteController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value="state", required=false) state: String
	) =
		EditAttendanceNoteCommand(student, point, user, Option(state))

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	) = {
		cmd.populate()
		form(cmd, student, academicYear, isIframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	) = {
		cmd.populate()
		form(cmd, student, academicYear)
	}

	private def form(
		cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		student: StudentMember,
		academicYear: AcademicYear,
		isIframe: Boolean = false
	) = {
		val mav = Mav("note/edit_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"returnTo" -> getReturnTo(Routes.View.student(student.homeDepartment, academicYear, student)),
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
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	) = {
		if (errors.hasErrors) {
			form(cmd, student, academicYear, isIframe = true)
		} else {
			cmd.apply()
			Mav("note/edit_note", "success" -> true, "isIframe" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringNote] with PopulateOnForm,
		errors: Errors,
		@PathVariable student: StudentMember,
		@PathVariable academicYear: AcademicYear
	) = {
		if (errors.hasErrors) {
			form(cmd, student, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.View.student(student.homeDepartment, academicYear, student))
		}
	}

}

@Controller
@RequestMapping(Array("/note/{academicYear}/bulk/{point}/edit"))
class BulkEditAttendanceNoteController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable academicYear: AcademicYear,
		@PathVariable point: AttendanceMonitoringPoint
	) = BulkAttendanceNoteCommand(point, user)


	private def form(
		cmd: Appliable[Seq[AttendanceMonitoringNote]] with PopulateOnForm,
		academicYear: AcademicYear,
		isAuto: Boolean = false
	) = {
		val mav = Mav("note/bulk_note",
			"allAbsenceTypes" -> AbsenceType.values,
			"isModal" -> ajax,
			"isAuto" -> isAuto
		)
		if(ajax)
			mav.noLayout()
		else
			mav.noNavigation()
	}

	@RequestMapping(method=Array(GET, HEAD, POST))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringNote]] with PopulateOnForm,
		@PathVariable academicYear: AcademicYear,
		@PathVariable point: AttendanceMonitoringPoint,
		@RequestParam(value="isAuto", required=false) isAuto: Boolean
	) = {
		cmd.populate()
		form(cmd, academicYear, isAuto)
	}

	@RequestMapping(method=Array(GET, HEAD, POST), params=Array("isSave"))
	def submitIframe(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringNote]] with PopulateOnForm,
		errors: Errors,
		@PathVariable academicYear: AcademicYear,
		@PathVariable point: AttendanceMonitoringPoint
	) = {
		if (errors.hasErrors) {
			form(cmd, academicYear)
		} else {
			cmd.apply()
			Mav("note/bulk_note", "success" -> true, "allAbsenceTypes" -> AbsenceType.values)
		}
	}

}