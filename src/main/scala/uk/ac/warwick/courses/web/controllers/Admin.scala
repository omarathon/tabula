package uk.ac.warwick.courses.web.controllers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.courses.data.model.Department
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.CurrentUser
import javax.validation.Valid
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.commands.assignments.AddAssignmentCommand
import collection.JavaConversions._

/**
 * Screens for department and module admins.
 */

@Controller
class AdminController extends Controllerism {

	@Autowired var moduleService: ModuleAndDepartmentService = _

	@RequestMapping(Array("/admin/"))
	def homeScreen(user: CurrentUser) =
		Mav("admin/home",
			"ownedDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions))

	@RequestMapping(Array("/admin/department/{dept}/"))
	def adminDepartment(@PathVariable dept: Department, user: CurrentUser): ModelAndView = {
		mustBeAbleTo(Manage(dept))
		Mav("admin/department",
			"department" -> dept,
			"modules" -> dept.modules.sortBy{ (module) => (module.assignments.isEmpty, module.code) })
	}
	
}
object AdminController {

	// Sub controllers

	@Controller
	class AddAssignmentController extends Controllerism {

		@ModelAttribute("addAssignment") def addAssignmentForm(@PathVariable module: Module) =
			new AddAssignmentCommand(definitely(module))

		@RequestMapping(value = Array("/admin/module/{module}/assignments/new"), method = Array(RequestMethod.GET))
		def addAssignmentForm(user: CurrentUser, @PathVariable module: Module,
				@ModelAttribute("addAssignment") form: AddAssignmentCommand, errors: Errors): ModelAndView = {
			mustBeAbleTo(Manage(module))
			Mav("admin/assignments/new",
				"department" -> module.department,
				"module" -> module)
		}

		@RequestMapping(value = Array("/admin/module/{module}/assignments/new"), method = Array(RequestMethod.POST))
		def addAssignmentSubmit(user: CurrentUser, @PathVariable module: Module,
				@Valid @ModelAttribute("addAssignment") form: AddAssignmentCommand, errors: Errors): ModelAndView = {
			mustBeAbleTo(Manage(module))
			if (errors.hasErrors) {
				addAssignmentForm(user, module, form, errors)
			} else {
				form.apply()
				Mav("redirect:/admin/department/" + module.department.code + "/#module-" + module.code)
			}
		}

	}

}

