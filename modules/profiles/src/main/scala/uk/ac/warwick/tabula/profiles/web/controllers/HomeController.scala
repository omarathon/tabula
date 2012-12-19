package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.web._
import uk.ac.warwick.tabula.web.controllers._
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand

@Controller class HomeController extends ProfilesController {

	var userLookup = Wire.auto[UserLookupService]
	def groupService = userLookup.getGroupService

	hideDeletedItems
	studentProfilesOnly
	
	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand = new SearchProfilesCommand

	@RequestMapping(Array("/")) def home() = Mav("home/view")
	
}