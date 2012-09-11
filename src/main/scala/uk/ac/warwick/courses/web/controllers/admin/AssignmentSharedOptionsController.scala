package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.courses.commands.assignments.SharedAssignmentPropertiesForm
import org.springframework.validation.Errors
import uk.ac.warwick.courses.data.model.Department
import javax.validation.Valid

/**
 * When setting up a batch of assignments using AddAssignmentsController, we need
 * to open a dialog for entering assignment settings. This controller shows that
 * form, does validation and then the resulting form fields are injected into the
 * original HTML page.
 */
@Controller
@RequestMapping(value=Array("/admin/department/{department}/shared-options"))
class AssignmentSharedOptionsController extends BaseController {

	@RequestMapping(method=Array(GET))
	def showForm(@ModelAttribute form: SharedAssignmentPropertiesForm, errors: Errors) = {
		mav(form)
	}

	@RequestMapping(method=Array(POST))
	def submitForm(@Valid @ModelAttribute form: SharedAssignmentPropertiesForm, errors: Errors) = {
		mav(form).addObjects(
			"submitted" -> true,
			"hasErrors" -> errors.hasErrors
		)
	}

	def mav(form: SharedAssignmentPropertiesForm) = {
		Mav("admin/assignments/shared_options",
			"department" -> form.department
		).noLayout()
	}

	@ModelAttribute
	def model(department: Department) = new SharedAssignmentPropertiesForm(department)

}
