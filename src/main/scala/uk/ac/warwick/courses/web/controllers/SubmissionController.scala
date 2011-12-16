package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Assignment
import collection.JavaConversions._
import uk.ac.warwick.courses.commands.assignments._
import org.joda.time.DateTime
import uk.ac.warwick.courses.helpers.DateTimeOrdering._
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.actions.View
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FeedbackDao

@Controller
@RequestMapping(Array("/module/{module}/"))
class ModuleController extends Controllerism {
  
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
class SubmissionController extends Controllerism {
	
	@Autowired var feedbackDao:FeedbackDao =_
	
	@ModelAttribute def form(@PathVariable assignment:Assignment, user:CurrentUser) = 
		new SubmitAssignmentCommand(assignment, user)
	
	@RequestMapping(method=Array(RequestMethod.GET))
	def view(@PathVariable module:Module, @PathVariable assignment:Assignment, user:CurrentUser, form:SubmitAssignmentCommand, errors:Errors) = {
		mustBeLinked(mandatory(assignment),  mandatory(module))
		mustBeAbleTo(View(assignment))
		
		Mav("submit/assignment", 
			"module"-> module,
			"assignment" -> assignment,
			"feedback" -> (if (assignment.resultsPublished) 
							  feedbackDao.getFeedbackByUniId(assignment, user.universityId)
						   else None)
		)
	}
	
	@RequestMapping(method=Array(RequestMethod.POST))
	def submit(@PathVariable module:Module, @PathVariable assignment:Assignment, user:CurrentUser, @Valid form:SubmitAssignmentCommand, errors:Errors) = {
		view(module,assignment,user,form,errors)
	}
			
}