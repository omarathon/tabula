package uk.ac.warwick.tabula.home.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.web._
import uk.ac.warwick.tabula.web.controllers._
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.permissions.Permissions

@Controller class HomeController extends BaseController {
	
	var moduleService = Wire[ModuleAndDepartmentService]

	hideDeletedItems

	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		val canAdmin =
			// Avoid doing too much work by just returning the first one of these that's true
			user.loggedIn && (
				moduleService.departmentsWithPermission(user, Permissions.Module.Administer).nonEmpty ||
				moduleService.departmentsWithPermission(user, Permissions.Route.Administer).nonEmpty ||
				moduleService.modulesWithPermission(user, Permissions.Module.Administer).nonEmpty ||
				moduleService.routesWithPermission(user, Permissions.Route.Administer).nonEmpty
			)
		
	  	Mav("home/view", 
	  		"ajax" -> ajax,
			"canAdmin" -> canAdmin,
			"jumbotron" -> true).noLayoutIf(ajax) // All hail our new Jumbotron overlords
	}
}