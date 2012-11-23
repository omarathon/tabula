package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ ModelAttribute, RequestMapping }
import uk.ac.warwick.tabula.coursework.commands.assignments.SharedAssignmentPropertiesForm
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import javax.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.data.model.Assignment

/**
 * When setting up a batch of assignments using AddAssignmentsController, we need
 * to open a dialog for entering assignment settings. This controller shows that
 * form, does validation and then the resulting form fields are injected into the
 * original HTML page.
 */
@Controller
@RequestMapping(value = Array("/admin/department/{department}/shared-options"))
class AssignmentSharedOptionsController extends CourseworkController {

	@RequestMapping(method = Array(GET))
	def showForm(@ModelAttribute form: SharedAssignmentPropertiesForm, errors: Errors, @PathVariable("department") department: Department) = {
		mav(form, department)
	}

	@RequestMapping(method = Array(POST))
	def submitForm(@Valid @ModelAttribute form: SharedAssignmentPropertiesForm, errors: Errors, @PathVariable("department") department: Department) = {
		mav(form, department).addObjects(
			"submitted" -> true,
			"hasErrors" -> errors.hasErrors)
	}

	def mav(form: SharedAssignmentPropertiesForm, @PathVariable("department") department: Department) = {
		Mav("admin/assignments/shared_options",
			"department" -> department,
			"maxWordCount" -> Assignment.MaximumWordCount).noLayout()
	}

	@ModelAttribute
	def model(department: Department) = new SharedAssignmentPropertiesForm()

}
