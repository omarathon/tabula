package uk.ac.warwick.tabula.web.controllers.attendance.profile.old

import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.profile.old.{OldAttendanceProfileInformation, OldProfileCommand}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/profile/{student}/2013"))
class OldProfileController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember)
		= OldProfileCommand(student, AcademicYear(2013))

	@RequestMapping
	def render(
		@ModelAttribute("command") cmd: Appliable[OldAttendanceProfileInformation],
		@PathVariable student: StudentMember,
		@RequestParam(value="expand", required=false) expand: Boolean
	) = {
		val info = cmd.apply()
		val baseMap = Map(
			"currentUser" -> user,
			"pointsByTerm" -> info.pointsData.pointsByTerm,
			"missedCountByTerm" -> info.missedCountByTerm,
			"nonReportedTerms" -> info.nonReportedTerms
		)

		if (ajax)
			Mav("attendance/profile/old/_profile", baseMap ++ Map("defaultExpand" -> expand)).noLayout()
		else
			Mav("attendance/profile/old/profile", baseMap ++ Map("defaultExpand" -> true)).crumbs(
				Breadcrumbs.Profile.Years(mandatory(student), user.apparentId == student.userId)
			)
	}
}
