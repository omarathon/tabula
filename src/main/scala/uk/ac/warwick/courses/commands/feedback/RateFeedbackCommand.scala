package uk.ac.warwick.courses.commands.feedback

import uk.ac.warwick.courses.commands.Command
import uk.ac.warwick.courses.commands.Description
import uk.ac.warwick.courses.data.model.Feedback
import scala.reflect.BeanProperty
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import uk.ac.warwick.courses.Features

class RateFeedbackCommand(val feedback:Feedback, val features:Features) extends Command[Unit] {
	
	@BeanProperty var rating:JInteger = Option(feedback).map{ _.ratingInteger }.orNull
	
	@BeanProperty var unset:Boolean = false
	
	def effectiveRating:JInteger = 
		if (unset) null
		else rating
	
	
	val maximumStars = 5
	
	@Transactional
	def apply {
		feedback.rating = 
			if (unset) None 
			else Some(rating)
	}
	
	// FIXME define validation messages
	def validate(errors:Errors) {
		if (enabled) {
			rating match {
				case r if r < 1 => errors.rejectValue("rating", "feedback.rating.low")
				case r if r > maximumStars => errors.rejectValue("rating", "feedback.rating.high")
				case null => errors.rejectValue("rating", "feedback.rating.empty")
				case _ =>
			}
		} else {
			errors.rejectValue("rating", "feedback.rating.disabled")
		}
	}
	
	def enabled = 
		features.collectRatings &&
		feedback.assignment.module.department.collectFeedbackRatings 
	
	def describe(d:Description) = d.feedback(feedback).properties(
			"rating" -> effectiveRating,
			"previousRating" -> feedback.rating.orNull
	)
}