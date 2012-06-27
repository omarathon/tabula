package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.commands.assignments.AddMarksCommand
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Participate
import org.springframework.validation.Errors
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/marks"))
class AddMarksController extends BaseController{
	
	@ModelAttribute def command(@PathVariable assignment:Assignment, user:CurrentUser) = new AddMarksCommand(assignment, user)
	
	//def onBind(cmd:AddMarksCommand) = cmd.onBind
	
	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module) = mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	
	@RequestMapping(method=Array(HEAD,GET))
	def uploadZipForm(@PathVariable module:Module, @PathVariable assignment:Assignment, @ModelAttribute cmd:AddMarksCommand):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		crumbed(Mav("admin/assignments/marks/marksform"), module)
	}
	
	@RequestMapping(method=Array(POST), params=Array("!confirm"))
	def confirmBatchUpload(@PathVariable module:Module, @PathVariable assignment:Assignment, @ModelAttribute cmd:AddMarksCommand, errors: Errors):Mav = {
		cmd.onBind
		cmd.postExtractValidation(errors)
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		crumbed(Mav("admin/assignments/marks/markspreview"), module)
	}
	
	@RequestMapping(method=Array(POST), params=Array("confirm=true"))
	def doUpload(@PathVariable module:Module, @PathVariable assignment:Assignment, @ModelAttribute cmd:AddMarksCommand, errors: Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		cmd.onBind
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}