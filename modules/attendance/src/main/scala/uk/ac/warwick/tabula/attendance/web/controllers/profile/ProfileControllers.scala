package uk.ac.warwick.tabula.attendance.web.controllers.profile

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.profile.old.{OldAttendanceProfileInformation, OldProfileCommand}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentMember

@Controller
@RequestMapping(value = Array("/profile"))
class ProfileHomeController extends AttendanceController {

	@RequestMapping
	def render() = user.profile match {
		case Some(student: StudentMember) =>
			Redirect(Routes.Profile.profileForYear(student, AcademicYear.guessByDate(DateTime.now)))
		case _ if user.isStaff =>
			Mav("profile/staff").noLayoutIf(ajax)
		case _ =>
			Mav("profile/unknown").noLayoutIf(ajax)
	}
}

@Controller
@RequestMapping(value = Array("/profile/{student}"))
class ProfileChooseYearController extends AttendanceController {

	@RequestMapping
	def render(@PathVariable student: StudentMember) =
		Mav("profile/years").noLayoutIf(ajax)
}

@Controller
@RequestMapping(value = Array("/profile/{student}/2013"))
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
			Mav("profile/old/_profile", baseMap ++ Map("defaultExpand" -> expand)).noLayout()
		else
			Mav("profile/old/profile", baseMap ++ Map("defaultExpand" -> true)).crumbs(
				Breadcrumbs.Profile.Years(mandatory(student))
			)
	}
}
