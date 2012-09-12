package uk.ac.warwick.courses.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.courses.data.model.{Module, Assignment}
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.ExtensionRequestCommand
import uk.ac.warwick.courses.web.Mav
import javax.validation.Valid
import org.springframework.validation.Errors

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}/extension"))
class ExtensionRequestController extends BaseController{

	@ModelAttribute
	def extensionRequestCommand(@PathVariable assignment:Assignment, user:CurrentUser) =
		new ExtensionRequestCommand(assignment, user)

	validatesWith{ (cmd:ExtensionRequestCommand, errors) => cmd.validate(errors) }

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
		= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment):Mav = {
		mustBeLinked(assignment,module)

		val model = Mav("submit/extension_request",
			"module" -> module,
			"assignment" -> assignment
		)
		crumbed(model, module)
	}

	@RequestMapping(method=Array(POST))
	def persistExtensionRequest(@PathVariable module:Module, @PathVariable assignment:Assignment,
								@Valid form:ExtensionRequestCommand, errors: Errors):Mav = {
		if(errors.hasErrors){
			showForm(module, assignment)
		}
		else {
			form.apply()
			val model = Mav("submit/extension_request_success",
				"module" -> module,
				"assignment" -> assignment
			)
			crumbed(model, module)
		}
	}
}
