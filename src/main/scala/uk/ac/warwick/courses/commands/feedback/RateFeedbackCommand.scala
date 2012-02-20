package uk.ac.warwick.courses.commands.feedback

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Feedback
import scala.reflect.BeanProperty
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import uk.ac.warwick.courses.Features

class RateFeedbackCommand(val feedback:Feedback, val features:Features) extends Command[Unit] {
	
	@BeanProperty var rating:JInteger =_
	
	@Transactional
	def apply {
		feedback.rating = Some(rating)
	}
	
	// FIXME define validation messages
	def validate(errors:Errors) {
		if (feedback.assignment.module.department.collectFeedbackRatings) {
			if (rating == null) {
				errors.rejectValue("rating", "feedback.rating.empty")
			} else if (rating < 1) {
				errors.rejectValue("rating", "feedback.rating.low")
			} else if (rating > 10) {
				errors.rejectValue("rating", "feedback.rating.high")
			}
		} else {
			errors.rejectValue("rating", "feedback.rating.disabled")
		}
	}
	
	def describe(d:Description) = d.feedback(feedback).properties(
			"rating" -> rating,
			"previousRating" -> feedback.rating
	)
}