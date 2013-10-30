package uk.ac.warwick.tabula.admin.web.controllers.modules
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.admin.web.Routes
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.admin.commands.modules.AddModuleCommand

@Controller
@RequestMapping(value = Array("/department/{dept}/module/new"))
class AddModuleController extends AdminController {

	// set up self validation for when @Valid is used
	validatesSelf[AddModuleCommand]

	@ModelAttribute
	def command(@PathVariable("dept") department: Department) = new AddModuleCommand(mandatory(department))
	
	@RequestMapping(method = Array(HEAD, GET))
	def showForm(cmd: AddModuleCommand, user: CurrentUser) = {
		Mav("admin/modules/add/form", 
			"department" -> cmd.department
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute cmd: AddModuleCommand, errors: Errors, user: CurrentUser) = {
		if (errors.hasErrors) {
			showForm(cmd, user)
		} else {
			val module = cmd.apply()
			Redirect(Routes.module(module))
		}
	}

}
