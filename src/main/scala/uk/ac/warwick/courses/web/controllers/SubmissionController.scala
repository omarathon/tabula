package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Module
import collection.JavaConversions._
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.assignments._
import org.joda.time.DateTime
import uk.ac.warwick.courses.helpers.DateTimeOrdering
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.actions.View
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid

@Controller
@RequestMapping(Array("/module/{module}/"))
class ModuleController extends Controllerism with DateTimeOrdering {
  
	def viewModule(@PathVariable module:Module) = {
		mustBeAbleTo(View(module))
		Mav("submit/module", 
			"module"-> definitely(module),
			"assignments" -> module.assignments.sortBy{ _.closeDate }.reverse)
	}
	
}

@Controller
@RequestMapping(Array("/module/{module}/{assignment}"))
class SubmissionController extends Controllerism {
	
	@ModelAttribute def form(@PathVariable assignment:Assignment, user:CurrentUser) = 
		new SubmitAssignmentCommand(assignment, user)
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def view(@PathVariable module:Module, @PathVariable assignment:Assignment, form:SubmitAssignmentCommand, errors:Errors) = {
		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(View(assignment))
		Mav("submit/assignment", 
			"module"-> definitely(module),
			"assignment" -> definitely(assignment))
	}
	
	@RequestMapping(method=Array(RequestMethod.POST))
	def submit(@PathVariable module:Module, @PathVariable assignment:Assignment, @Valid form:SubmitAssignmentCommand, errors:Errors) = {
		view(module,assignment,form,errors)
	}
			
}