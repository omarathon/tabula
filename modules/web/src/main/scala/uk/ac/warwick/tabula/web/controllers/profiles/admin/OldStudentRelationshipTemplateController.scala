package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.commands.profiles.relationships.OldStudentRelationshipTemplateCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/{relationshipType}/allocate-old/template"))
class OldStudentRelationshipTemplateController extends ProfilesController {
	@ModelAttribute def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		new OldStudentRelationshipTemplateCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def generateTemplate(cmd: OldStudentRelationshipTemplateCommand) = cmd.apply()
}
