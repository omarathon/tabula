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
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

@Controller class HomeController extends ProfilesController {
	
	var moduleService = Wire.auto[ModuleAndDepartmentService]

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)) orNull

	@RequestMapping(Array("/")) def home() = {
		if (user.isStaff) {
			Mav(
				"home/view",
				"isAPersonalTutor" -> currentMember.isAPersonalTutor,
				"adminDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions)
			)
		} else if (optionalCurrentMember.isDefined && currentMember.userType == Student) {
			Redirect(Routes.profile.view(currentMember))
		} else Mav("home/nopermission")
	}
}