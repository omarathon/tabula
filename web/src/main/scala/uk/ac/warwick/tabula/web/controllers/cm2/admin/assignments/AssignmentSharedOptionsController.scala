package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.cm2.assignments.SharedAssignmentPropertiesForm
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

/**
	* When setting up a batch of assignments using AddSitsAssignmentsController, we need
	* to open a dialog for entering assignment settings. This controller shows that
	* form, does validation and then the resulting form fields are injected into the
	* original HTML page.
	*/
@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/shared-options"))
class AssignmentSharedOptionsController extends CourseworkController {

	@ModelAttribute("sharedAssignmentPropertiesForm")
	def model() = new SharedAssignmentPropertiesForm

	@RequestMapping
	def showForm(@ModelAttribute("sharedAssignmentPropertiesForm") form: SharedAssignmentPropertiesForm): Mav =
		Mav("cm2/admin/assignments/shared_options",
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes
		).noLayout()

	@RequestMapping(method = Array(POST))
	def submitForm(@Valid @ModelAttribute("sharedAssignmentPropertiesForm") form: SharedAssignmentPropertiesForm, errors: Errors): Mav =
		showForm(form).addObjects(
			"submitted" -> true,
			"hasErrors" -> errors.hasErrors
		)

}
