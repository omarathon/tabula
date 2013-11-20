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
import uk.ac.warwick.tabula.admin.commands.department.AddSubDepartmentCommand
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Appliable

@Controller
@RequestMapping(value = Array("/department/{department}/subdepartment/new"))
class AddSubDepartmentController extends AdminController {

	validatesSelf[SelfValidating]
	
	@ModelAttribute("allFilterRules") 
	def allFilterRules = Department.FilterRule.allFilterRules

	@ModelAttribute("addSubDepartmentCommand")
	def command(@PathVariable("department") department: Department) = AddSubDepartmentCommand(mandatory(department))
	
	@RequestMapping(method = Array(HEAD, GET))
	def showForm() = Mav("admin/department/add/form")

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("addSubDepartmentCommand") command: Appliable[Department], errors: Errors) = {
		if (errors.hasErrors) showForm()
		else {
			val subDepartment = command.apply()
			Redirect(Routes.department(subDepartment))
		}
	}

}
