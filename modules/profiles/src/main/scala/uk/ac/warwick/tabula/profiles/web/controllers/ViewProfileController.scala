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


class ViewProfileCommand(profile: StudentMember) extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile)

@Controller
@RequestMapping(Array("/view/{member}"))
class ViewProfileController extends ProfilesController {
	
	@ModelAttribute("searchProfilesCommand")
	def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)) orNull
	
	@ModelAttribute("viewMeetingRecordCommand")
	def viewMeetingRecordCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => restricted(new ViewMeetingRecordCommand(student))
		case _ => None
	}
	
	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(student)
		case _ => throw new ItemNotFoundException
	}


	@RequestMapping
	def viewProfile(
			@ModelAttribute("viewProfileCommand") profileCmd: ViewProfileCommand,
			@ModelAttribute("viewMeetingRecordCommand") meetingsCmd: Option[ViewMeetingRecordCommand]) = {
		
		val profiledStudentMember = profileCmd.apply
		val isSelf = (profiledStudentMember.universityId == user.universityId)
		
		val meetings = meetingsCmd match {
			case None => Seq()
			case Some(cmd) => cmd.apply
		}

		Mav("profile/view", 
		    "profile" -> profiledStudentMember,
		    "viewer" -> currentMember,
		    "isSelf" -> isSelf,
		    "meetings" -> meetings)
		   .crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}
}

