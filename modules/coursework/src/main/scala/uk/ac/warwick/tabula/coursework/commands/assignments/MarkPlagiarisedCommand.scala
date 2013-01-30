package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.helpers.ArrayList
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._

class MarkPlagiarisedCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with SelfValidating {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.ManagePlagiarismStatus(), assignment)

	var assignmentService = Wire.auto[AssignmentService]

	//@BeanProperty var submissions: JList[Submission] = ArrayList()
    @BeanProperty var students: JList[String] = ArrayList()
	@BeanProperty var confirm: Boolean = false
	@BeanProperty var markPlagiarised: Boolean = true
	
	var submissionsUpdated = 0

	def applyInternal() {
    	    for (uniId <- students;
             submission <- assignmentService.getSubmissionByUniId(assignment, uniId)) {
                    submission.suspectPlagiarised = markPlagiarised
                    assignmentService.saveSubmission(submission)
                    submissionsUpdated = submissionsUpdated + 1
            }
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

	override def describe(d: Description) = d
        .assignment(assignment)
        .property("students" -> students)
        
	
	override def describeResult(d: Description) = d
	    .assignment(assignment)
		//.property("submissionCount" -> submissions.size)
		.property("submissionCount" -> submissionsUpdated)
		.property("markPlagiarised" -> markPlagiarised)
}
