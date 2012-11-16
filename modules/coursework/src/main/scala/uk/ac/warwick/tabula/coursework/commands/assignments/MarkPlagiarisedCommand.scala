package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment
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


class MarkPlagiarisedCommand(val assignment: Assignment) extends Command[JList[Submission]] with SelfValidating {

  var assignmentService = Wire.auto[AssignmentService]

  @BeanProperty var submissions: JList[Submission] = ArrayList()
	@BeanProperty var confirm: Boolean = false
	@BeanProperty var markPlagiarised: Boolean = true

	def work() = {
		for (submission <- submissions) {
			submission.suspectPlagiarised = markPlagiarised
			//zipService.invalidateSubmissionZip(assignment)

			assignmentService.saveSubmission(submission)
		}
		submissions
	}

	def prevalidate(errors: Errors) {
		if (submissions.find(_.assignment != assignment).isDefined) {
			errors.reject("submission.bulk.wrongassignment")
		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) errors.rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

	def describe(d: Description) = 
		d.assignment(assignment)
		.property("submissionCount" -> submissions.size)
		.property("markPlagiarised" -> markPlagiarised)
}
