package uk.ac.warwick.tabula.admin.web.controllers.department

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.admin.commands.department.DisplaySettingsCommand
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.commands.{PopulateOnForm, SelfValidating, Appliable}
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(Array("/department/{dept}/settings/display"))
class DisplaySettingsController extends AdminController {
	
	var relationshipService = Wire[RelationshipService]

	type DisplaySettingsCommand = Appliable[Department] with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("displaySettingsCommand")
	def displaySettingsCommand(@PathVariable("dept") dept:Department): DisplaySettingsCommand = DisplaySettingsCommand(mandatory(dept))

	@ModelAttribute("allRelationshipTypes") def allRelationshipTypes = relationshipService.allStudentRelationshipTypes

	@RequestMapping(method=Array(GET, HEAD))
	def initialView(@PathVariable("dept") dept: Department, @ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand) = {
		cmd.populate()
		viewSettings(dept)
	}
	
	private def viewSettings(dept: Department) =
			crumbed(Mav("admin/display-settings",
				"department" -> dept,
				"returnTo" -> getReturnTo("")
			), dept)

	@RequestMapping(method=Array(POST))
	def saveSettings(@Valid @ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand, errors:Errors, @PathVariable("dept") department: Department) = {
		if (errors.hasErrors){
			viewSettings(department)
		} else {
			cmd.apply()
			Redirect(Routes.department(department))
		}
	}
}