package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.commands.{ProfilesHomeCommand, ProfilesHomeInformation, SearchProfilesCommand}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService

@Controller class HomeController extends ProfilesController with AgentTrait {
	
	var departmentService = Wire[ModuleAndDepartmentService]

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull
		
	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = ProfilesHomeCommand(user, optionalCurrentMember)

	@RequestMapping(Array("/")) def home(@ModelAttribute("command") cmd: Appliable[ProfilesHomeInformation]) = {

		if (!user.isPGR && isAgent(user.userId) && optionalCurrentMember.exists(_.userType == Student)) {
			Redirect(Routes.profile.view(currentMember))
		} else {
			val info = cmd.apply()

			Mav("home/view",
				"relationshipTypesMap" -> info.relationshipTypesMap,
				"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
				"universityId" -> user.universityId,
				"isPGR" -> user.isPGR,
				"smallGroups" -> info.smallGroups,
				"adminDepartments" -> info.adminDepartments,
				"searchDepartments" -> departmentService.departmentsWithPermission(user, Permissions.Profiles.Search)
			)
		}
	}
	
	@RequestMapping(Array("/view")) def redirectHome() = Redirect(Routes.home)
}