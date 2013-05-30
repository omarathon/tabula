package uk.ac.warwick.tabula.admin.web.controllers.department

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import uk.ac.warwick.tabula.web.Breadcrumbs
import uk.ac.warwick.tabula.admin.commands.department.DisplaySettingsCommand
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.admin.web.Routes
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping(Array("/department/{dept}/settings/display"))
class DisplaySettingsController extends AdminController {

	@ModelAttribute def displaySettingsCommand(@PathVariable("dept") dept:Department) = new DisplaySettingsCommand(dept)
	
	validatesSelf[DisplaySettingsCommand]

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(GET, HEAD))
	def viewSettings(
		@PathVariable("dept") dept: Department, 
		user: CurrentUser,  
		cmd:DisplaySettingsCommand, errors:Errors) =
			crumbed(Mav("admin/display-settings",
				"department" -> dept,
				"returnTo" -> getReturnTo("")
			), dept)

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid cmd:DisplaySettingsCommand, errors:Errors) = {
		if (errors.hasErrors){
			viewSettings(cmd.department, user, cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.department(cmd.department))
		}
	}
}