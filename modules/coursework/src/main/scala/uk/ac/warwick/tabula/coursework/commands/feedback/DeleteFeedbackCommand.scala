package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.coursework.commands.Command
import uk.ac.warwick.tabula.coursework.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.data.model.Feedback
import uk.ac.warwick.tabula.helpers.ArrayList
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.data.FeedbackDao
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.SelfValidating
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.spring.Wire

class DeleteFeedbackCommand(val assignment: Assignment) extends Command[Unit] with SelfValidating {

	var feedbackDao = Wire.auto[FeedbackDao]

	@BeanProperty var feedbacks: JList[Feedback] = ArrayList()
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