package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.commands.relationships.AllocateStudentsToRelationshipCommand
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

/**
 * Allocates students to relationships in a department.
 */
@Controller
@RequestMapping(value=Array("/department/{department}/{relationshipType}/allocate"))
class AllocateStudentsToRelationshipController extends ProfilesController {
	
	validatesSelf[AllocateStudentsToRelationshipCommand]
	
	@ModelAttribute
	def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) = 
		new AllocateStudentsToRelationshipCommand(department, relationshipType, user)

	@RequestMapping
	def showForm(cmd: AllocateStudentsToRelationshipCommand) = {
		cmd.populate()
		form(cmd)
	}
	
	@RequestMapping(method=Array(POST), params=Array("action=refresh"))
	def form(cmd: AllocateStudentsToRelationshipCommand) = {
		cmd.sort()
		Mav("relationships/allocate")
	}
	
	@RequestMapping(method = Array(POST), params = Array("isfile=true", "action!=refresh"))
	def previewFileUpload(@PathVariable("department") department: Department, @Valid cmd: AllocateStudentsToRelationshipCommand, errors: Errors): Mav = {
		if (errors.hasErrors && errors.getFieldError.getCode == "file.wrongtype.one") {
			form(cmd)
		} else {
			Mav("relationships/upload_preview")
		}
	}

	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd: AllocateStudentsToRelationshipCommand, errors: Errors): Mav = {
		if (errors.hasErrors()) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.relationships(cmd.department, cmd.relationshipType))
		}
	}

}