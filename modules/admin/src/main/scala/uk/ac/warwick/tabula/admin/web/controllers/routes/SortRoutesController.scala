package uk.ac.warwick.tabula.admin.web.controllers.routes

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.admin.commands.routes.{SortRoutesCommandState, SortRoutesCommand}
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects, SelfValidating}

/**
 * The interface for sorting department routes into
 * child departments.
 */
@Controller
@RequestMapping(value=Array("/department/{department}/sort-routes"))
class SortRoutesController extends AdminController {

	type SortRoutesCommand = Appliable[Unit] with GroupsObjects[Route, Department] with SortRoutesCommandState
	validatesSelf[SelfValidating]
	
	@ModelAttribute("sortRoutesCommand")
	def command(@PathVariable department: Department): SortRoutesCommand = SortRoutesCommand(department)

	@RequestMapping(method=Array(GET, HEAD))
	def showForm(@ModelAttribute("sortRoutesCommand") cmd: SortRoutesCommand):Mav = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("sortRoutesCommand") cmd: SortRoutesCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			form(cmd).addObjects("saved" -> true)
		}
	}
		
	private def form(@ModelAttribute("sortRoutesCommand") cmd: SortRoutesCommand): Mav = {
		if (!cmd.department.hasChildren && cmd.department.hasParent) {
			// Sorting is done from the POV of the parent department.
			Redirect(Routes.department.sortRoutes(cmd.department.parent))
		} else {
			Mav("admin/routes/arrange/form")
		}
	}
	
}
