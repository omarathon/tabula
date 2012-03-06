package uk.ac.warwick.courses.web.controllers
import scala.collection.JavaConversions.asScalaBuffer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import uk.ac.warwick.courses.actions.View
import uk.ac.warwick.courses.commands.assignments.SubmitAssignmentCommand
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.helpers.DateTimeOrdering.orderedDateTime
import uk.ac.warwick.courses.CurrentUser
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.courses.web.Routes
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.courses.commands.assignments.SubmittedFieldsPropertyEditor
import org.springframework.transaction.annotation.Transactional

@Controller
@RequestMapping(Array("/module/{module}/"))
class ModuleController extends BaseController {
  
	@RequestMapping
	def viewModule(@PathVariable module:Module) = {
		mustBeAbleTo(View(mandatory(module)))
		Mav("submit/module", 
			"module"-> module,
			"assignments" -> module.assignments.sortBy{ _.closeDate }.reverse)
	}
	
}

@Controller
@RequestMapping(Array("/module/{module}/{assignment}"))
class AssignmentController extends AbstractAssignmentController {
	
	hideDeletedItems
	
	validatesWith{ (cmd:SubmitAssignmentCommand,errors) => cmd.validate(errors) }
	
	@ModelAttribute def form(@PathVariable("module") module:Module, @PathVariable("assignment") assignment:Assignment, user:CurrentUser) = {  
		val cmd = new SubmitAssignmentCommand(assignment, user)
		cmd.module = module
		cmd
	}
	
	
	def checks(form:SubmitAssignmentCommand) = {
		mustBeLinked(mandatory(form.assignment),  mandatory(form.module))
	}
	
	@RequestMapping(method=Array(GET))
	def view(user:CurrentUser, form:SubmitAssignmentCommand, errors:Errors) = {
		val assignment = form.assignment
		val module = form.module
		form.onBind
		checks(form)
		
		val feedback = checkCanGetFeedback(assignment, user)
		
		if (user.loggedIn) {
			Mav("submit/assignment", 
				"module"-> module,
				"assignment" -> assignment,
				"feedback" -> feedback
			)
		} else {
			RedirectToSignin() 
		}
	}
	
	@Transactional
	@RequestMapping(method=Array(POST))
	def submit(@PathVariable module:Module, user:CurrentUser, @Valid form:SubmitAssignmentCommand, errors:Errors) = {
		val assignment = form.assignment
		val module = form.module
		form.onBind
		checks(form)
		if (errors.hasErrors) {
			view(user,form,errors)
		} else {
			form.apply
			Redirect(Routes.assignment(form.assignment))
		}
	}
			
}