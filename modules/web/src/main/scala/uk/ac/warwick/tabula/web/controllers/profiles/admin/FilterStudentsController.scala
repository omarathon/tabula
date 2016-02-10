package uk.ac.warwick.tabula.web.controllers.profiles.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, CurrentSITSAcademicYear, SelfValidating}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.profiles.{FilterStudentsCommand, FilterStudentsResults}
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value=Array("/profiles/department/{department}/students"))
class FilterStudentsController extends ProfilesController with CurrentSITSAcademicYear {

	validatesSelf[SelfValidating]

	@ModelAttribute("filterStudentsCommand")
	def command(@PathVariable department: Department) =
		FilterStudentsCommand(department, academicYear)

	@RequestMapping
	def filter(@Valid @ModelAttribute("filterStudentsCommand") cmd: Appliable[FilterStudentsResults], errors: Errors, @PathVariable department: Department) = {
		if (errors.hasErrors) {
			Mav("profiles/profile/filter/filter").noLayout()
		}
		else {
			val results = cmd.apply()
			val modelMap = Map(
				"students" -> results.students,
				"totalResults" -> results.totalResults,
				"academicYear" -> academicYear
			)
			if (ajax) Mav("profiles/profile/filter/results", modelMap).noLayout()
			else Mav("profiles/profile/filter/filter", modelMap)
		}
	}

}