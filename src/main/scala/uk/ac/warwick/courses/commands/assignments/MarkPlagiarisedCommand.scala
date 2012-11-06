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

	def prevalidate(implicit errors: Errors) {
		if (submissions.find(_.assignment != assignment).isDefined) {
			reject("submission.bulk.wrongassignment")
		}
	}

	def validate(implicit errors: Errors) {
		prevalidate
		if (!confirm) rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

	def describe(d: Description) = d
		.assignment(assignment)
		.property("submissionCount" -> submissions.size)
		.property("markPlagiarised" -> markPlagiarised)
}
