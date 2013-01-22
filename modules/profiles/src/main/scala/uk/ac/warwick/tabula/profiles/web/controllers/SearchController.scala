package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.web.ProfileBreadcrumbs
import uk.ac.warwick.tabula.data.model.Member
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.actions.Search
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire

@Controller
class SearchController extends ProfilesController {
	
	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand = new SearchProfilesCommand(currentMember, user)
	
	@RequestMapping(value=Array("/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: SearchProfilesCommand) = Mav("profile/search/form")
	
	@RequestMapping(value=Array("/search"), params=Array("query"))
	def submit(@Valid @ModelAttribute cmd: SearchProfilesCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			Mav("profile/search/results",
				"results" -> cmd.apply())
		}
	}
	
	@RequestMapping(value=Array("/search.json"), params=Array("query"))
	def submitJson(@Valid @ModelAttribute cmd: SearchProfilesCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val profilesJson: JList[Map[String, Object]] = toJson(cmd.apply())
			
			Mav(new JSONView(profilesJson))
		}
	}
	
	def toJson(profiles: Seq[Member]) = {
		def memberToJson(member: Member) = Map[String, String](
			"name" -> member.fullName,
			"id" -> member.universityId,
			"userId" -> member.userId,
			"description" -> member.description)
			
		profiles.map(memberToJson(_))
	}

}