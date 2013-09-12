package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.commands.relationships.StudentRelationshipTemplateCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

@Controller
@RequestMapping(value = Array("/department/{department}/{relationshipType}/template"))
class StudentRelationshipTemplateController extends ProfilesController {
	@ModelAttribute def command(@PathVariable("department") department: Department, @PathVariable("relationshipType") relationshipType: StudentRelationshipType) =
		new StudentRelationshipTemplateCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def generateTemplate(cmd: StudentRelationshipTemplateCommand) = cmd.apply()
}
