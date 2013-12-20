package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.profiles.commands.ProfilesHomeCommand
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.profiles.web.Routes
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.model.MemberUserType._
import uk.ac.warwick.tabula.profiles.commands.ProfilesHomeInformation
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.spring.Wire

@Controller class HomeController extends ProfilesController {
	
	var departmentService = Wire[ModuleAndDepartmentService]

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull
		
	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = ProfilesHomeCommand(user, optionalCurrentMember)

	@RequestMapping(Array("/")) def home(@ModelAttribute("command") cmd: Appliable[ProfilesHomeInformation]) = {
		if (user.isStaff) {
			val info = cmd.apply()

			Mav("home/view",
				"relationshipTypesMap" -> info.relationshipTypesMap,
				"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
				"universityId" -> user.universityId,
				"isPGR" -> user.isPGR,
				"smallGroups" -> info.smallGroups,
				"adminDepartments" -> info.adminDepartments,
				"currentUserDepartment" -> departmentService.getDepartmentByCode(user.departmentCode.toLowerCase)
			)
		} else if (optionalCurrentMember.filter(_.userType == Student).isDefined) {
			Redirect(Routes.profile.view(currentMember))
		} else Mav("home/nopermission")
	}
	
	@RequestMapping(Array("/view")) def redirectHome() = Redirect(Routes.home)
}