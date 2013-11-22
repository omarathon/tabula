package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.ProfileCommand
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.attendance.web.Routes
import org.joda.time.DateTime
import uk.ac.warwick.tabula.attendance.commands.AttendanceProfileInformation

@Controller
@RequestMapping(value = Array("/profile"))
class ProfileHomeController extends AttendanceController with AutowiringProfileServiceComponent {

	@RequestMapping
	def render() = profileService.getMemberByUserId(user.apparentId) match {
		case Some(student: StudentMember) => Redirect(Routes.profile(student, AcademicYear.guessByDate(DateTime.now)))
		case _ if user.isStaff => Mav("profile/profile_staff").noLayoutIf(ajax)
		case _ => Mav("profile/profile_unknown").noLayoutIf(ajax)
	}
}

@Controller
@RequestMapping(value = Array("/profile/{student}/{academicYear}"))
class ProfileController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember, @PathVariable academicYear: AcademicYear)
		= ProfileCommand(student, academicYear)

	@RequestMapping
	def render(
		@ModelAttribute("command") cmd: Appliable[Option[AttendanceProfileInformation]],
		@RequestParam(value="expand", required=false) expand: Boolean
	) = {
		val info = cmd.apply()
		val baseMap = Map(
			"currentUser" -> user,
			"monitoringPointsByTerm" -> info.map { _.monitoringPointsByTerm },
			"checkpointState" -> info.map { _.checkpointState },
			"missedCountByTerm" -> info.map { _.missedCountByTerm }
		)

		if (ajax)
			Mav("profile/_profile", baseMap ++ Map("defaultExpand" -> expand)).noLayout()
		else
			Mav("profile/profile", baseMap ++ Map("defaultExpand" -> true))
	}
}
