package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._

import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module


class DeleteFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with SelfValidating {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Delete, assignment)

	var feedbackDao = Wire[FeedbackDao]

	var feedbacks: JList[Feedback] = JArrayList()
	var confirm: Boolean = false

	def applyInternal() = {
		for (feedback <- feedbacks) feedbackDao.delete(feedback)
	}

	def prevalidate(errors: Errors) {
		if (feedbacks.find(_.assignment != assignment).isDefined) {
			errors.reject("feedback.delete.wrongassignment")
		}
		// HFC-88 allow deleting released feedback.
		//		if (feedbacks.find(_.released).isDefined) {
		//			reject("feedback.delete.released")
		//		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) errors.rejectValue("confirm", "feedback.delete.confirm")
	}

	def describe(d: Description) = 
		d.assignment(assignment)
		.property("feedbackCount" -> feedbacks.size)

}