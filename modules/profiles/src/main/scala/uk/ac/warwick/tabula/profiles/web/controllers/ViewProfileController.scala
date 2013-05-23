package uk.ac.warwick.tabula.profiles.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService


class ViewProfileCommand(user: CurrentUser, profile: StudentMember) extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile) with Logging {
	if (user.isStudent && user.universityId != profile.universityId) {
		logger.info("Denying access for user " + user + " to view profile " + profile)
		throw new PermissionDeniedException(user, Permissions.Profiles.Read.Core, profile)
	}
}

@Controller
@RequestMapping(Array("/view/{member}"))
class ViewProfileController extends ProfilesController {

	var userLookup = Wire.auto[UserLookupService]

	@ModelAttribute("searchProfilesCommand")
	def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => restricted(new ViewMeetingRecordCommand(student, user))
		case _ => None
	}

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping
	def viewProfile(
			@ModelAttribute("viewProfileCommand") profileCmd: ViewProfileCommand,
			@ModelAttribute("viewMeetingRecordCommand") meetingsCmd: Option[ViewMeetingRecordCommand],
			@RequestParam(value="meeting", required=false) openMeetingId: String,
			@RequestParam(defaultValue="", required=false) tutorId: String) = {

		val profiledStudentMember = profileCmd.apply
		val isSelf = (profiledStudentMember.universityId == user.universityId)

		val meetings = meetingsCmd match {
			case None => Seq()
			case Some(cmd) => cmd.apply
		}

		val openMeeting = meetings.find(m => m.id == openMeetingId).getOrElse(null)

		val tutor = userLookup.getUserByWarwickUniId(tutorId)

		Mav("profile/view",
			"profile" -> profiledStudentMember,
			"viewer" -> currentMember,
			"isSelf" -> isSelf,
			"hasCurrentEnrolment" -> profiledStudentMember.hasCurrentEnrolment,
			"meetings" -> meetings,
			"openMeeting" -> openMeeting,
			"tutor" -> tutor)
		.crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}
}
