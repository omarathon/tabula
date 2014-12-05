package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.profiles.commands.ViewModuleRegistrationsCommand
import uk.ac.warwick.tabula.commands.Appliable

@Controller
@RequestMapping(Array("/view/modules/{studentCourseDetails}/{academicYear}"))
class ViewModuleRegistrationsController
	extends ProfilesController {

	@ModelAttribute("command")
	def command(@PathVariable studentCourseDetails: StudentCourseDetails, @PathVariable academicYear: AcademicYear) =
		ViewModuleRegistrationsCommand(mandatory(studentCourseDetails), academicYear)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[ModuleRegistration]]) = {
		Mav("profile/module_list",
		  "moduleRegs" -> cmd.apply()
		).noLayoutIf(ajax)
	}

}