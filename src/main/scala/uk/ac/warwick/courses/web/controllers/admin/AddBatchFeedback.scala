package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.Controllerism
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Participate
import javax.validation.Valid

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/feedback/batch"))
class AddBatchFeedback extends Controllerism {
	@ModelAttribute def command(@PathVariable assignment:Assignment, user:CurrentUser) = 
		new AddFeedbackCommand(assignment, user)
	
	def onBind(cmd:AddFeedbackCommand) = cmd.onBind
	
//	validatesWith { (command:AddFeedbackCommand, errors:Errors) =>
//		command.validation(errors)
//	}
	
//	override def binding(binder:WebDataBinder, command:AddFeedbackCommand) {
//		command.onBind
//		//cmd.onBind
//	}
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def uploadZipForm(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			@ModelAttribute cmd:AddFeedbackCommand):Mav = {
		//cmd.onBind
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		Mav("admin/assignments/feedback/zipform")
	}
	
	@RequestMapping(method=Array(RequestMethod.POST), params=Array("!confirm"))
	def confirmBatchUpload(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			@ModelAttribute cmd:AddFeedbackCommand, errors: Errors):Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadZipForm(module,assignment,cmd)
		} else {
			cmd.onBind
			cmd.postExtractValidation(errors)
			mustBeLinked(assignment,module)
			mustBeAbleTo(Participate(module))
			Mav("admin/assignments/feedback/zipreview")
		}
	}
	
	@RequestMapping(method=Array(RequestMethod.POST), params=Array("confirm=true"))
	def doUpload(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			@ModelAttribute cmd:AddFeedbackCommand, errors: Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		cmd.preExtractValidation(errors)
		cmd.onBind
		cmd.postExtractValidation(errors)
		if (errors.hasErrors) {
			Mav("admin/assignments/feedback/zipreview")
		} else {
			// do apply, redirect back
			//cmd.apply()
			Mav("admin/assignments/feedback/zipreview")
		}
	}
	
	
}