package uk.ac.warwick.tabula.admin.web.controllers.routes

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import javax.validation.Valid
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.admin.commands.routes.SortRoutesCommand

/**
 * The interface for sorting department routes into
 * child departments.
 */
@Controller
@RequestMapping(value=Array("/department/{department}/sort-routes"))
class SortRoutesController extends AdminController {

	validatesSelf[SortRoutesCommand]
	
	@ModelAttribute
	def command(@PathVariable department: Department) = new SortRoutesCommand(department)

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute cmd: SortRoutesCommand, errors: Errors):Mav = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute cmd: SortRoutesCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors()) {
			form(cmd)
		} else {
			cmd.apply()
			form(cmd).addObjects("saved" -> true)
		}
	}
		
	def form(cmd: SortRoutesCommand): Mav = {
		if (cmd.department.hasParent) {
			// Sorting is done from the POV of the top department.
			Redirect(Routes.department.sortRoutes(cmd.department.parent))
		} else {
			Mav("admin/routes/arrange/form")
		}
	}
	
}
