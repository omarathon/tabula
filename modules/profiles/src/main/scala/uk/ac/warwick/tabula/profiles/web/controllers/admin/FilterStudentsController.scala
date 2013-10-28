package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.profiles.commands.FilterStudentsCommand
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Appliable
import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.profiles.commands.FilterStudentsResults

@Controller
@RequestMapping(value=Array("/department/{department}/students"))
class FilterStudentsController extends ProfilesController {
	
	validatesSelf[SelfValidating]
	
	@ModelAttribute("filterStudentsCommand")
	def command(@PathVariable department: Department) = 
		FilterStudentsCommand(department)
		
	@RequestMapping
	def filter(@Valid @ModelAttribute("filterStudentsCommand") cmd: Appliable[FilterStudentsResults], errors: Errors, @PathVariable department: Department) = {
		if (errors.hasErrors()) {
			Mav("profile/filter/filter")
		} else {
			val results = cmd.apply()
			
			if (ajax) Mav("profile/filter/results", "students" -> results.students, "totalResults" -> results.totalResults).noLayout()
			else Mav("profile/filter/filter",
				"students" -> results.students,
				"totalResults" -> results.totalResults
			)
		}
	}

}