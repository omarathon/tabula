package uk.ac.warwick.tabula.admin.web.controllers.modules
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.{Module, Department}
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.admin.commands.modules.AddModuleCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}

@Controller
@RequestMapping(value = Array("/department/{dept}/module/new"))
class AddModuleController extends AdminController {

	// set up self validation for when @Valid is used
	type AddModuleCommand = Appliable[Module]
	validatesSelf[SelfValidating]

	@ModelAttribute
	def command(@PathVariable("dept") department: Department): AddModuleCommand = AddModuleCommand(mandatory(department))
	
	@RequestMapping(method = Array(HEAD, GET))
	def showForm(cmd: AddModuleCommand, @PathVariable("dept") department: Department) = {
		Mav("admin/modules/add/form", 
			"department" -> department
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute cmd: AddModuleCommand, errors: Errors, @PathVariable("dept") department: Department) = {
		if (errors.hasErrors) {
			showForm(cmd, department)
		} else {
			val module = cmd.apply()
			Redirect(Routes.module(module))
		}
	}

}
