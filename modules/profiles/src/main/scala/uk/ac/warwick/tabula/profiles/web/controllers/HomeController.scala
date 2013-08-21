package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.{Features, CurrentUser}
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.tabula.services.{SmallGroupService, UserLookupService, ProfileService, ModuleAndDepartmentService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web._
import uk.ac.warwick.tabula.web.controllers._
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.permissions.Permissions

@Controller class HomeController extends ProfilesController {
	
	var moduleService = Wire[ModuleAndDepartmentService]
	var smallGroupsService = Wire[SmallGroupService]
	var features = Wire[Features]

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand =
		restricted(new SearchProfilesCommand(currentMember, user)).orNull

	@RequestMapping(Array("/")) def home() = {
		if (user.isStaff) {
			val smallGroups =
				if (features.smallGroupTeachingTutorView) smallGroupsService.findSmallGroupsByTutor(user.apparentUser)
				else Nil

			Mav("home/view",
				"isAPersonalTutor" -> currentMember.isAPersonalTutor,
				"isASupervisor" -> currentMember.isASupervisor,
				"universityId" -> currentMember.universityId,
				"isPGR" -> user.isPGR,
				"smallGroups" -> smallGroups,
				"adminDepartments" -> moduleService.departmentsWithPermission(user, Permissions.Department.ManageProfiles)
			)
		} else if (optionalCurrentMember.isDefined && currentMember.userType == Student) {
			Redirect(Routes.profile.view(currentMember))
		} else Mav("home/nopermission")
	}
}