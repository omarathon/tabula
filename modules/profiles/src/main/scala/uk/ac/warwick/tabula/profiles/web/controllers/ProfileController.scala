package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.actions.View
import uk.ac.warwick.tabula.profiles.web.ProfileBreadcrumbs
import uk.ac.warwick.tabula.web.Breadcrumbs
import uk.ac.warwick.tabula.commands.imports.ImportProfilesCommand
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.actions.Create

@Controller
@RequestMapping(Array("/view/{member}"))
class ProfileController extends BaseController with ProfileBreadcrumbs {
  
	hideDeletedItems
	
	@RequestMapping
	def viewProfile(@PathVariable member: Member) = {
		mustBeAbleTo(View(mandatory(member)))
		Mav("profile/view", 
		    "profile" -> member)
		   .crumbs(Breadcrumbs.Profile(member))
	}
	
	@RequestMapping(value=Array("/reimport"), method=Array(POST))
	def reimport(@PathVariable member: Member) = {
		// Sysadmins only
		if (!user.sysadmin) throw new PermissionDeniedException(user, Create())
	  
		val command = new ImportProfilesCommand
		command.refresh(member)
		
		Redirect("/view/" + member.universityId)
	}

}