package uk.ac.warwick.courses.commands.assignments

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.helpers.ArrayList
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.SelfValidating
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.data.model.Submission

@Configurable
class DeleteSubmissionCommand(val assignment:Assignment) extends Command[Unit] with SelfValidating {
	
	@BeanProperty var submissions:JList[Submission] = ArrayList() 
	
	@Autowired var assignmentService:AssignmentService = _
	
	@BeanProperty var confirm:Boolean = false
	
	def apply() = {
		for (submission <- submissions) assignmentService.delete(submission)
	}
	
	def prevalidate(implicit errors:Errors) {
		if (submissions.find(_.assignment != assignment).isDefined) {
			reject("submission.delete.wrongassignment")
		}
	}
	
	def validate(implicit errors:Errors) {
		prevalidate
		if (!confirm) rejectValue("confirm", "submission.delete.confirm")
	}
	
	def describe(d:Description) = d
			.assignment(assignment)
			.property("submissionCount" -> submissions.size)
	
	
}