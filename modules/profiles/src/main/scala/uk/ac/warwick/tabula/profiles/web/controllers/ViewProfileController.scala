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


class ViewProfileCommand(profile: StudentMember) extends ViewViewableCommand(Permissions.Profiles.Read.Core, profile)

@Controller
@RequestMapping(Array("/view/{member}"))
class ViewProfileController extends ProfilesController {
	
	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)) orNull
	
	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(student)
		case _ => throw new ItemNotFoundException
	}


	@RequestMapping
	def viewProfile(@ModelAttribute("viewProfileCommand") cmd: ViewProfileCommand) = {
		val profiledStudentMember = cmd.apply
		
		val isSelf = (profiledStudentMember.universityId == user.universityId)
		
		Mav("profile/view", 
		    "profile" -> profiledStudentMember,
		    "viewer" -> currentMember,
		    "isSelf" -> isSelf,
		    "hasCurrentEnrolment" -> profiledStudentMember.hasCurrentEnrolment)
		   .crumbs(Breadcrumbs.Profile(profiledStudentMember, isSelf))
	}
}
