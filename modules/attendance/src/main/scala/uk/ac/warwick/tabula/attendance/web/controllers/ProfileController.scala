package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, RuntimeMember}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.ProfileCommand
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.attendance.web.Routes
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.attendance.commands.AttendanceProfileInformation

@Controller
@RequestMapping(value = Array("/profile"))
class ProfileHomeController extends AttendanceController with AutowiringProfileServiceComponent {

	@RequestMapping
	def render() = profileService.getMemberByUserId(user.apparentId) match {
		case Some(student: StudentMember) => 
			student.mostSignificantCourseDetails match {
				case Some(scd) => Redirect(Routes.profile(scd, AcademicYear.guessByDate(DateTime.now)))
				case None => throw new ItemNotFoundException()
			}
		case _ if user.isStaff => Mav("home/profile_staff").noLayoutIf(ajax)
		case _ => Mav("home/profile_unknown").noLayoutIf(ajax)
	}
}

@Controller
@RequestMapping(value = Array("/profile/{student}/{academicYear}"))
class ProfileController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember, @PathVariable academicYear: AcademicYear)
		= ProfileCommand(student, academicYear)

	@RequestMapping
	def render(@ModelAttribute("command") cmd: Appliable[Option[AttendanceProfileInformation]]) = {
		val info = cmd.apply()
		val baseMap = Map(
			"currentUser" -> user,
			"monitoringPointsByTerm" -> info.map { _.monitoringPointsByTerm },
			"checkpointState" -> info.map { _.checkpointState },
			"missedCountByTerm" -> info.map { _.missedCountByTerm }
		)

		if (ajax)
			Mav("home/_profile", baseMap).noLayout()
		else
			Mav("home/profile", baseMap ++ Map("defaultExpand" -> true))
	}
}
