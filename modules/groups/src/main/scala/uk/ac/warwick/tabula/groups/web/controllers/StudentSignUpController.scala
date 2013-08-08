package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.groups.commands.{DeallocateSelfFromGroupValidator, AllocateSelfToGroupValidator, DeallocateSelfFromGroupCommand, AllocateSelfToGroupCommand}
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.CurrentUser
import javax.validation.Valid
import uk.ac.warwick.tabula.groups.web.Routes


@Controller
class StudentSignUpController extends GroupsController {

	validatesSelf[AllocateSelfToGroupValidator]

	@ModelAttribute("command")
	def command(@PathVariable("set_id") groupSet:SmallGroupSet, user:CurrentUser):Appliable[SmallGroupSet]={
		AllocateSelfToGroupCommand(user.apparentUser, groupSet)
	}

	@RequestMapping(method=Array(POST),value =Array("/module/{module_code}/groups/{set_id}/signup"))
	def signUp(@Valid @ModelAttribute("command") command:Appliable[SmallGroupSet]): Mav = {
		command.apply()
		Redirect(Routes.home)
	}
}
@Controller
class StudentUnSignUpController extends GroupsController {

	validatesSelf[DeallocateSelfFromGroupValidator]

	@ModelAttribute("command")
	def command(@PathVariable("set_id") groupSet:SmallGroupSet, user:CurrentUser):Appliable[SmallGroupSet]={
		DeallocateSelfFromGroupCommand(user.apparentUser, groupSet)
	}

	@RequestMapping(method=Array(POST),value =Array("/module/{module_code}/groups/{set_id}/leave"))
	def signUp(@Valid @ModelAttribute("command") command:Appliable[SmallGroupSet]): Mav = {
		command.apply()
		Redirect(Routes.home)
	}
}
