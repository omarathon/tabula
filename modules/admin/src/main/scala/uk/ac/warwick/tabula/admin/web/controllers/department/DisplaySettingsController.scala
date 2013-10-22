package uk.ac.warwick.tabula.admin.web.controllers.department

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import uk.ac.warwick.tabula.admin.commands.department.{DisplaySettingsCommand, DisplaySettingsCommandState}
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire


@Controller
@RequestMapping(Array("/department/{dept}/settings/display"))
class DisplaySettingsController extends AdminController {
	
	var relationshipService = Wire[RelationshipService]

	validatesSelf[SelfValidating]

	@ModelAttribute("displaySettingsCommand") def displaySettingsCommand(@PathVariable("dept") dept:Department) = DisplaySettingsCommand(dept)

	@RequestMapping(method=Array(GET, HEAD))
	def initialView(@PathVariable("dept") dept: Department, 
		@ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand with Appliable[Department], errors: Errors) = {
		cmd.init()
		viewSettings(dept)
	}
	
	def viewSettings(dept: Department) =
			crumbed(Mav("admin/display-settings",
				"department" -> dept,
				"allRelationshipTypes" -> relationshipService.allStudentRelationshipTypes,
				"returnTo" -> getReturnTo("")
			), dept)

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid @ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand with Appliable[Department], errors:Errors) = {
		if (errors.hasErrors){
			viewSettings(cmd.department)
		} else {
			cmd.apply()
			Redirect(Routes.department(cmd.department))
		}
	}
}