package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam}
import uk.ac.warwick.tabula.commands.profiles.ViewProfileCommand
import uk.ac.warwick.tabula.services.AutowiringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.web.Routes

class OldViewProfileByStudentController
	extends OldViewProfileController
	with AutowiringMeetingRecordServiceComponent {

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case staff: StaffMember => new ViewProfileCommand(user, staff)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping(Array("/profiles/view/{member}"))
	def viewProfile(
		@PathVariable member: Member,
		@ModelAttribute("viewProfileCommand") profileCmd: Appliable[Member],
		@RequestParam(value = "meeting", required = false) openMeetingId: String,
		@RequestParam(defaultValue = "", required = false) agentId: String): Mav = {
		val profiledMember = profileCmd.apply()

		// used in pattern guard and in result, so use lazy val to evaluate only once
		lazy val meetingForAnotherScyd: Option[(StudentCourseYearDetails, AbstractMeetingRecord)] = profiledMember.asInstanceOf[StudentMember].mostSignificantCourseDetails.map { s =>
			val meeting = meetingRecordService.get(openMeetingId)
			meeting.flatMap { m =>
				val latestScyd = s.latestStudentCourseYearDetails
				val meetingScyd = meetingRecordService.getAcademicYear(m).flatMap(studentCourseYearFromYear(s, _))
			  meetingScyd.filter(_ != latestScyd).map((_, m))
			}
		} getOrElse(None)

		profiledMember match {
			case studentProfile: StudentMember if meetingForAnotherScyd.isDefined =>
				Redirect(Routes.oldProfile.view(meetingForAnotherScyd.get._1, meetingForAnotherScyd.get._2))
			case studentProfile: StudentMember =>
				viewProfileForCourse(studentProfile.mostSignificantCourseDetails,
						studentProfile.defaultYearDetails,
						openMeetingId,
						agentId,
						studentProfile)
			case staffProfile: StaffMember => viewProfileForStaff(staffProfile)
			case _ => throw new ItemNotFoundException
		}
	}
}


class OldViewMyProfileController extends OldViewProfileController {
	@RequestMapping(Array("/profiles/view/me"))
	def viewMe(currentUser: CurrentUser) = {
		currentUser.profile map { profile =>
			Redirect(Routes.oldProfile.view(profile))
		} getOrElse {
			Redirect(Routes.home)
		}
	}
}
