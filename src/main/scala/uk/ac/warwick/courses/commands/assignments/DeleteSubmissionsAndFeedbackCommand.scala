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

@Configurable
class DeleteSubmissionsAndFeedbackCommand(val assignment: Assignment) extends Command[Unit] with SelfValidating {

	//@BeanProperty var submissions: JList[Submission] = ArrayList()
    @BeanProperty var students: JList[String] = ArrayList()
	
	@Autowired var assignmentService: AssignmentService = _
	@Autowired var zipService: ZipService = _
    @Autowired var feedbackDao: FeedbackDao = _
    
	@BeanProperty var confirm: Boolean = false

	var submissionsDeleted = 0
	var feedbacksDeleted = 0

	def apply() = {
    	// delete the submissions
        for (uniId <- students;
        	 submission <- assignmentService.getSubmissionByUniId(assignment, uniId)) {
                    assignmentService.delete(submission)
                    submissionsDeleted = submissionsDeleted + 1
            }
            
        // delete the feedbacks
        for (uniId <- students;
             feedback <- feedbackDao.getFeedbackByUniId(assignment, uniId)) {
                    feedbackDao.delete(feedback)
                    feedbacksDeleted = feedbacksDeleted + 1
            }

		zipService.invalidateSubmissionZip(assignment)
		zipService.invalidateFeedbackZip(assignment)
	}

	def prevalidate(implicit errors: Errors) {
        for (uniId <- students;
             submission <- assignmentService.getSubmissionByUniId(assignment, uniId)) {
                if (submission.assignment != assignment) reject("submission.bulk.wrongassignment")
            }
            
        // delete the feedbacks
        for (uniId <- students;
             feedback <- feedbackDao.getFeedbackByUniId(assignment, uniId)) {
                if (feedback.assignment != assignment) reject("feedback.bulk.wrongassignment")
            }		
	}

	def validate(implicit errors: Errors) {
		prevalidate
		if (!confirm) rejectValue("confirm", "submission.delete.confirm")
	}

	override def describe(d: Description) = d
		.assignment(assignment)
		.property("students" -> students)
        
    override def describeResult(d: Description) = d
        .assignment(assignment)
        .property("submissionsDeleted" -> submissionsDeleted)
        .property("feedbacksDeleted" -> feedbacksDeleted)
}
