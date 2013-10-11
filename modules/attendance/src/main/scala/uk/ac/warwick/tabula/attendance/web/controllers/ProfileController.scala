package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, RuntimeMember, StudentCourseDetails}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.ProfileCommand
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.attendance.web.Routes
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.Member

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
@RequestMapping(value = Array("/profile/{studentCourseDetails}/{academicYear}"))
class ProfileController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear)
		= ProfileCommand(studentCourseDetails, academicYear)

	@RequestMapping
	def render(@ModelAttribute("command") cmd: Appliable[Unit]) = {
		cmd.apply()

		if (ajax)
			Mav("home/_profile", "currentUser" -> user).noLayout()
		else
			Mav("home/profile", "currentUser" -> user, "defaultExpand" -> true)
	}
}
