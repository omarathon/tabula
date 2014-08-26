package uk.ac.warwick.tabula.attendance.web.controllers.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.profile.{ViewSmallGroupsForPointCommand, ViewSmallGroupsForPointCommandResult}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.ListStudentGroupAttendanceCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint

@Controller
@RequestMapping(Array("/profile/{student}/{academicYear}/{point}/groups"))
class ViewSmallGroupsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable student: StudentMember,
		@PathVariable point: AttendanceMonitoringPoint,
		@PathVariable academicYear: AcademicYear
	) =
		ViewSmallGroupsForPointCommand(
			mandatory(student),
			mandatory(point),
			ListStudentGroupAttendanceCommand(mandatory(student), mandatory(academicYear)).apply()
		)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[ViewSmallGroupsForPointCommandResult]) = {
		val result = cmd.apply()
		Mav("profile/groups",
			"result" -> result,
			"currentMember" -> currentMember
		).noLayoutIf(ajax)
	}
}