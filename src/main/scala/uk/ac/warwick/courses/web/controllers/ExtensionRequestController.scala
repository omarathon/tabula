package uk.ac.warwick.courses.web.controllers

import scala.collection.JavaConversions._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.courses.data.model.{Module, Assignment}
import uk.ac.warwick.courses.{PermissionDeniedException, CurrentUser}
import uk.ac.warwick.courses.web.Mav
import org.springframework.validation.Errors
import uk.ac.warwick.courses.commands.assignments.extensions.ExtensionRequestCommand
import uk.ac.warwick.courses.data.model.forms.Extension
import uk.ac.warwick.courses.commands.assignments.extensions.messages.NewExtensionRequestMessage
import uk.ac.warwick.courses.actions.{View, Create}

@Controller
@RequestMapping(value=Array("/module/{module}/{assignment}/extension"))
class ExtensionRequestController extends BaseController{

	@ModelAttribute
	def extensionRequestCommand(@PathVariable assignment:Assignment, user:CurrentUser) =
		new ExtensionRequestCommand(assignment, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module)
		= mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))

	@RequestMapping(method=Array(HEAD,GET))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment,
				 @ModelAttribute cmd:ExtensionRequestCommand):Mav = {
		mustBeLinked(assignment,module)
		if (!module.department.canRequestExtension)
			throw new PermissionDeniedException(user, View(assignment))
		else {
			if (user.loggedIn){
				assignment.findExtension(user.universityId).foreach(cmd.presetValues(_))
				Mav("submit/extension_request",
					"module" -> module,
					"assignment" -> assignment,
					"department" -> module.department,
					"command" -> cmd
				)
			} else {
				RedirectToSignin()
			}
		}
	}

	@RequestMapping(method=Array(POST))
	def persistExtensionRequest(@PathVariable module:Module, @PathVariable assignment:Assignment,
								cmd:ExtensionRequestCommand, errors: Errors):Mav = {
		cmd.onBind()
		cmd.validate(errors)
		if(errors.hasErrors){
			showForm(module, assignment, cmd)
		}
		else {
			val extension = cmd.apply()
			sendExtensionRequestMessage(extension, cmd.modified)
			val model = Mav("submit/extension_request_success",
				"module" -> module,
				"assignment" -> assignment
			)
			crumbed(model, module)
		}
	}

	def sendExtensionRequestMessage(extension: Extension, modified:Boolean){
		val assignment = extension.assignment
		val moduleManagers = assignment.module.participants.includeUsers
		if (modified){
			//TODO-RITCHIE separate message for modified requests
		} else {
			moduleManagers.foreach(new NewExtensionRequestMessage(extension, _).apply())
		}
	}
}
