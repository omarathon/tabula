package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentCourseYearDetails, StudentCourseDetails, Member, StudentMember}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav

@Controller
class ViewProfileByStudentController extends ViewProfileController {

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case staff: StaffMember => new ViewProfileCommand(user, staff)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping(Array("/view/{member}"))
	def viewProfile(
		@PathVariable("member") member: Member,
		@ModelAttribute("viewProfileCommand") profileCmd: Appliable[Member],
		@RequestParam(value = "meeting", required = false) openMeetingId: String,
		@RequestParam(defaultValue = "", required = false) agentId: String) = {
		val profiledMember = profileCmd.apply()

		profiledMember match {
			case studentProfile: StudentMember => viewProfileForCourse(studentProfile.mostSignificantCourseDetails,
				studentProfile.defaultYearDetails,
				openMeetingId,
				agentId,
				studentProfile)
			case staffProfile: StaffMember => viewProfileForStaff(staffProfile)
		}
	}


}

@Controller
class ViewMyProfileController extends ViewProfileController {
	@RequestMapping(Array("/view/me"))
	def viewMe(currentUser: CurrentUser) = {
		currentUser.profile map { profile =>
			Redirect(Routes.profile.view(profile))
		} getOrElse {
			Redirect(Routes.home)
		}
	}
}
