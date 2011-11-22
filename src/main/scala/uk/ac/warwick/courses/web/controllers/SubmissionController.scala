package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Module
import collection.JavaConversions._
import uk.ac.warwick.courses.data.model.Assignment
import org.joda.time.DateTime
import uk.ac.warwick.courses.helpers.DateTimeOrdering
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.actions.View

@Controller
class SubmissionController extends Controllerism with DateTimeOrdering {
  
	@RequestMapping(Array("/module/{module}"))
	def viewModule(@PathVariable module:Module) = {
		mustBeAbleTo(View(module))
		Mav("submit/module", 
			"module"-> definitely(module),
			"assignments" -> module.assignments.sortBy{ _.closeDate }.reverse
				)
	}
			
	@RequestMapping(Array("/module/{module}/{assignment}"))
	def viewAssignment(@PathVariable module:Module, @PathVariable assignment:Assignment) = {
		if (assignment.module != module) throw new ItemNotFoundException
		mustBeAbleTo(View(assignment))
		Mav("submit/assignment", 
			"module"-> definitely(module),
			"assignment" -> definitely(assignment))
	}
			
}