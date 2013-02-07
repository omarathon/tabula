package uk.ac.warwick.tabula.profiles.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.ViewViewableCommand

class ViewProfileCommand(member: Member) extends ViewViewableCommand(Permissions.Profiles.Read, member)

@Controller
@RequestMapping(Array("/view/{member}"))
class ViewProfileController extends ProfilesController {
	
	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)) orNull
	
	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = new ViewProfileCommand(member)
	
	@RequestMapping
	def viewProfile(@ModelAttribute("viewProfileCommand") cmd: ViewProfileCommand) = {
		val profiledMember = cmd.apply
		
		val isSelf = (profiledMember.universityId == user.universityId)
		
		Mav("profile/view", 
		    "profile" -> profiledMember,
		    "viewer" -> currentMember,
		    "isSelf" -> isSelf)
		   .crumbs(Breadcrumbs.Profile(profiledMember, isSelf))
	}

}