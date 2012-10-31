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
import uk.ac.warwick.courses.services.ZipService
import uk.ac.warwick.courses.JList

@Configurable
class MarkPlagiarisedCommand(val assignment: Assignment) extends Command[Unit] with SelfValidating {

	//@BeanProperty var submissions: JList[Submission] = ArrayList()
    @BeanProperty var students: JList[String] = ArrayList()
    
	@Autowired var assignmentService: AssignmentService = _
	//@Autowired var zipService:ZipService = _

	@BeanProperty var confirm: Boolean = false

	@BeanProperty var markPlagiarised: Boolean = true
	
	var submissionsUpdated = 0

	def apply() {
    	for (uniId <- students) {
    	    val submissionOption = assignmentService.getSubmissionByUniId(assignment, uniId)
    	    
    	    submissionOption match {
    	    	case Some(submission) => {
    	    		submission.suspectPlagiarised = markPlagiarised
    	    		assignmentService.saveSubmission(submission)
    	    		submissionsUpdated = submissionsUpdated + 1
    	    	}
    	    	case None => //nothing (no submission exists for this student)
    	    }
		}
	}

	def validate(implicit errors: Errors) {
		if (!confirm) rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

	def describe(d: Description) = d
		.assignment(assignment)
		//.property("submissionCount" -> submissions.size)
		.property("submissionCount" -> submissionsUpdated)
		.property("markPlagiarised" -> markPlagiarised)
}
