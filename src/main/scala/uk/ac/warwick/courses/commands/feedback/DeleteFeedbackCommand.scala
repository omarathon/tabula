package uk.ac.warwick.courses.commands.feedback

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

@Configurable
class DeleteFeedbackCommand(val assignment: Assignment) extends Command[Unit] with SelfValidating {

	@BeanProperty var feedbacks: JList[Feedback] = ArrayList()

	@Autowired var feedbackDao: FeedbackDao = _

	@BeanProperty var confirm: Boolean = false

	def work() = {
		for (feedback <- feedbacks) feedbackDao.delete(feedback)
	}

	def prevalidate(implicit errors: Errors) {
		if (feedbacks.find(_.assignment != assignment).isDefined) {
			reject("feedback.delete.wrongassignment")
		}
		// HFC-88 allow deleting released feedback.
		//		if (feedbacks.find(_.released).isDefined) {
		//			reject("feedback.delete.released")
		//		}
	}

	def validate(implicit errors: Errors) {
		prevalidate
		if (!confirm) rejectValue("confirm", "feedback.delete.confirm")
	}

	def describe(d: Description) = d
		.assignment(assignment)
		.property("feedbackCount" -> feedbacks.size)

}