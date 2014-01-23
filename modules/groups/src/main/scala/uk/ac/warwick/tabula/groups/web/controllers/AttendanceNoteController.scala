package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.{ApplyWithCallback, PopulateOnForm, Appliable}
import org.springframework.validation.Errors
import scala.Array
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.fileserver.{FileServer, RenderableFile}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.{SmallGroupService, ProfileService}
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.groups.commands.{EditAttendanceNoteCommand, AttendanceNoteAttachmentCommand}
import uk.ac.warwick.tabula.groups.web.Routes

@Controller
@RequestMapping(Array("/note/{student}/{occurrence}"))
class AttendanceNoteController extends GroupsController {

	@Autowired var smallGroupService: SmallGroupService = _
	@Autowired var profileService: ProfileService = _

	@RequestMapping
	def home(
		@PathVariable student: StudentMember,
		@PathVariable occurrence: SmallGroupEventOccurrence
	) = {
		val attendanceNote = smallGroupService.getAttendanceNote(student.universityId, occurrence).getOrElse(throw new ItemNotFoundException())
		val attendance = smallGroupService.getAttendance(student.universityId, occurrence).getOrElse(null)
		Mav("groups/attendance/view_note",
			"attendanceNote" -> attendanceNote,
			"attendance" -> attendance,
			"updatedBy" -> profileService.getAllMembersWithUserId(attendanceNote.updatedBy).headOption.map{_.fullName}.getOrElse(""),
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		).noLayoutIf(ajax)
	}

}

@Controller
@RequestMapping(Array("/note/{student}/{occurrence}/attachment/{fileName}"))
class AttendanceNoteAttachmentController extends GroupsController {

	@Autowired var fileServer: FileServer = _

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable occurrence: SmallGroupEventOccurrence) =
		AttendanceNoteAttachmentCommand(student, occurrence, user)

	@RequestMapping
	def get(@ModelAttribute("command") cmd: ApplyWithCallback[Option[RenderableFile]])
		(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		cmd.callback = { (renderable) => renderable.map{fileServer.serve} }
		cmd.apply().orElse { throw new ItemNotFoundException() }
	}

}

@Controller
@RequestMapping(Array("/note/{student}/{occurrence}/edit"))
class EditAttendanceNoteController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember, @PathVariable occurrence: SmallGroupEventOccurrence) =
		EditAttendanceNoteCommand(student, occurrence, user)

	@RequestMapping(method=Array(GET, HEAD), params=Array("isIframe"))
	def getIframe(
		@ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm
	) = {
		cmd.populate()
		form(cmd, isIframe = true)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(
		@ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		@PathVariable student: StudentMember
	) = {
		cmd.populate()
		form(cmd)
	}

	private def form(
		cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		isIframe: Boolean = false
	) = {
		val mav = Mav("groups/attendance/edit_note",
			"returnTo" -> getReturnTo(Routes.tutor.mygroups(user.apparentUser)),
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
		@ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		errors: Errors
	) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("groups/attendance/edit_note", "success" -> true)
		}
	}

	@RequestMapping(method=Array(POST))
	def submit(
		@ModelAttribute("command") cmd: Appliable[SmallGroupEventAttendanceNote] with PopulateOnForm,
		errors: Errors
	) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.tutor.mygroups(user.apparentUser))
		}
	}

}
