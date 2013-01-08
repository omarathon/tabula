package uk.ac.warwick.tabula.profiles.web.controllers

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import javax.persistence.Entity
import uk.ac.warwick.tabula.actions.View
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand

@Controller
@RequestMapping(Array("/view/{member}"))
class ViewProfileController extends ProfilesController {
	
	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand = new SearchProfilesCommand(currentMember)
	
	@RequestMapping
	def viewProfile(@PathVariable member: Member) = {
		mustBeAbleTo(View(mandatory(member)))
		
		val isSelf = (member.universityId == user.universityId)
		
		Mav("profile/view", 
		    "profile" -> member,
		    "isSelf" -> isSelf)
		   .crumbs(Breadcrumbs.Profile(member, isSelf))
	}

}