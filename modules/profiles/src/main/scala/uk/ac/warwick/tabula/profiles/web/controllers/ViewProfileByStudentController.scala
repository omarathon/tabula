package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.{ Member, StudentMember }
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.profiles.web.Routes

@Controller
class ViewProfileByStudentController extends ViewProfileController {

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping(Array("/view/{member}"))
	def viewProfile(
		@PathVariable("member") member: Member,
		@ModelAttribute("viewProfileCommand") profileCmd: Appliable[StudentMember],
		@RequestParam(value = "meeting", required = false) openMeetingId: String,
		@RequestParam(defaultValue = "", required = false) agentId: String) = {
		val profiledStudentMember = profileCmd.apply()
		viewProfileForCourse(profiledStudentMember.mostSignificantCourseDetails, openMeetingId, agentId, profiledStudentMember)
	}

	@RequestMapping(Array("/view/me"))
	def viewMe(currentUser: CurrentUser) = {
		currentUser.profile map { profile =>
			Redirect(Routes.profile.view(profile))
		} getOrElse {
			Redirect(Routes.home)
		}
	}
}
