package uk.ac.warwick.tabula.web.controllers.groups

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.web.views.JSONView


@Controller
@RequestMapping(value=Array("/groups/module/{module_code}/groups/{set_id}/signup"))
class StudentSignUpController extends GroupsController with AutowiringSmallGroupServiceComponent {

	validatesSelf[AllocateSelfToGroupValidator]

	type StudentSelfSignUpCommand = Appliable[SmallGroupSet] with StudentSignUpCommandState

	@ModelAttribute("command")
	def command(@PathVariable("set_id") groupSet:SmallGroupSet, user:CurrentUser):Appliable[SmallGroupSet] = {
		AllocateSelfToGroupCommand(user.apparentUser, groupSet)
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def ajaxClashTimetableInfo(@ModelAttribute("command") command: StudentSelfSignUpCommand): Mav = {
		val clash = smallGroupService.doesTimetableClashesForStudent(command.group, command.user)
		Mav(new JSONView(Map("clash" -> clash)))
	}


	@RequestMapping(method=Array(POST))
	def signUp(@Valid @ModelAttribute("command") command:Appliable[SmallGroupSet], errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav("groups/signup/problems", "action" -> "signup")
		} else {
			command.apply()
			Redirect(Routes.home)
		}
	}
}

@RequestMapping(value=Array("/groups/module/{module_code}/groups/{set_id}/leave"))
@Controller
class StudentUnSignUpController extends GroupsController {

	validatesSelf[DeallocateSelfFromGroupValidator]

	@ModelAttribute("command")
	def command(@PathVariable("set_id") groupSet:SmallGroupSet, user:CurrentUser):Appliable[SmallGroupSet]={
		DeallocateSelfFromGroupCommand(user.apparentUser, groupSet)
	}

	@RequestMapping(method=Array(GET, HEAD))
	def get(): Mav = Redirect(Routes.home)

	@RequestMapping(method=Array(POST))
	def signUp(@Valid @ModelAttribute("command") command:Appliable[SmallGroupSet], errors: Errors): Mav = {
		if (errors.hasErrors) {
			Mav("groups/signup/problems", "action" -> "leave")
		} else {
			command.apply()
			Redirect(Routes.home)
		}
	}
}
