package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.{AttendanceMonitoringService, UserLookupService}
import uk.ac.warwick.tabula.helpers.DateBuilder

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
		val checkpoint = monitoringPointService.getCheckpoints(Seq(point), student).head._2
		Mav("note/view_note",
			"attendanceNote" -> attendanceNote,
			"checkpoint" -> checkpoint,
			"updatedBy" -> userLookup.getUserByUserId(attendanceNote.updatedBy).getFullName,
			"updatedDate" -> DateBuilder.format(attendanceNote.updatedDate),
			"isModal" -> ajax
		).noLayoutIf(ajax)
	}

}