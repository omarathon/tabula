package uk.ac.warwick.tabula.coursework.web.controllers.admin.modules

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.coursework.commands.modules.SortModulesCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid

/**
 * The interface for sorting department modules into
 * child departments (or module groups, whatever you want to call them).
 */
@Controller
@RequestMapping(value=Array("/admin/department/{department}/sort-modules"))
class SortModulesController extends BaseController {

	validatesSelf[SortModulesCommand]
	
	@ModelAttribute
	def command(@PathVariable department: Department) = new SortModulesCommand(department)

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute cmd:SortModulesCommand):Mav = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute cmd:SortModulesCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors()) {
			form(cmd)
		} else {
			cmd.apply()
			form(cmd).addObjects("saved" -> true)
		}
	}
		
	def form(cmd:SortModulesCommand):Mav = {
		if (cmd.department.hasParent) {
			// Sorting is done from the POV of the top department.
			Redirect(Routes.admin.department(cmd.department.parent) + "sort-modules")
		} else {
			Mav("admin/modules/arrange/form")
		}
	}
	
}