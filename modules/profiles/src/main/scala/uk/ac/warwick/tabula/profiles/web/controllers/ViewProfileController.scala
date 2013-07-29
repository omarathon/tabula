package uk.ac.warwick.tabula.profiles.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{SmallGroupService, UserLookupService}
import uk.ac.warwick.tabula.data.model.{RelationshipType, MeetingRecord, Member, StudentMember}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.{Appliable, ViewViewableCommand}
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand


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
	var smallGroupService = Wire[SmallGroupService]

	@ModelAttribute("searchProfilesCommand")
	def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

	@ModelAttribute("viewTutorMeetingRecordCommand")
	def viewTutorMeetingRecordCommand(@PathVariable("member") member: Member) = {
		member.mostSignificantCourseDetails match {
			case Some(scd) => restricted(ViewMeetingRecordCommand(scd, user, RelationshipType.PersonalTutor))
			case _ => {
				logger.warn("Member " + member.universityId + " has no most significant course details")
				None
			}
		}
	}

	@ModelAttribute("viewSupervisorMeetingRecordCommand")
	def viewSupervisorMeetingRecordCommand(@PathVariable("member") member: Member) = {
		member.mostSignificantCourseDetails match {
			case Some(scd) => restricted(ViewMeetingRecordCommand(scd, user, RelationshipType.Supervisor))
			case _ => {
				logger.warn("Member " + member.universityId + " has no most significant course details")
				None
			}
		}
	}

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping
	def viewProfile(
			@ModelAttribute("viewProfileCommand") profileCmd: Appliable[StudentMember],
			@ModelAttribute("viewTutorMeetingRecordCommand") tutorMeetingsCmd: Option[Appliable[Seq[MeetingRecord]]],
			@ModelAttribute("viewSupervisorMeetingRecordCommand") supervisorMeetingsCmd: Option[Appliable[Seq[MeetingRecord]]],
			@RequestParam(value="meeting", required=false) openMeetingId: String,
			@RequestParam(defaultValue="", required=false) tutorId: String) = {

		val profiledStudentMember = profileCmd.apply
		val isSelf = (profiledStudentMember.universityId == user.universityId)

		val tutorMeetings = tutorMeetingsCmd match {
			case None => Seq()
			case Some(cmd) => cmd.apply()
		}
		val supervisorMeetings = supervisorMeetingsCmd match {
			case None => Seq()
			case Some(cmd) => cmd.apply()
		}

		val openMeeting = tutorMeetings.find(m => m.id == openMeetingId).getOrElse(null)

		val tutor = userLookup.getUserByWarwickUniId(tutorId)

		// the number of small groups that the student is a member of
		val numSmallGroups = smallGroupService.findSmallGroupsByStudent(profiledStudentMember.asSsoUser).size

		Mav("profile/view",
			"profile" -> profiledStudentMember,
			"viewer" -> currentMember,
			"isSelf" -> isSelf,
			"tutorMeetings" -> tutorMeetings,
		  "supervisorMeetings"->supervisorMeetings,
			"openMeeting" -> openMeeting,
			"numSmallGroups" -> numSmallGroups,
			"tutor" -> tutor)
		.crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}
}
