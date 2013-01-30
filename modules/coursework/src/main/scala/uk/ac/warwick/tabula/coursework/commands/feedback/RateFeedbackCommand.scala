package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Feedback
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.Assignment


/**
 * A holder for a boolean value plus an extra flag to say that
 * we want it to be null. This is because Spring won't bind null
 * to a value, it will leave it with its existing value.
 */
case class NullableBoolean(@BeanProperty var value: JBoolean) {
	@BeanProperty var unset: Boolean = _
	def toBoolean: Option[Boolean] = if (unset) None else Option(value)

	def update {
		if (unset) value = null
	}
}

class RateFeedbackCommand(val module: Module, val assignment: Assignment, val feedback: Feedback) extends Command[Unit] {
	def this(module: Module, assignment: Assignment, opt: Option[Feedback]) {
		this(module, assignment, opt.orNull)
	}
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Feedback.Rate(), feedback)
	
	var features = Wire.auto[Features]

	//	@BeanProperty var rating:JInteger = _ 

	@BeanProperty var wasPrompt: NullableBoolean =
		NullableBoolean(Option(feedback).flatMap(_.ratingPrompt))

	@BeanProperty var wasHelpful: NullableBoolean =
		NullableBoolean(Option(feedback).flatMap(_.ratingHelpful))

	//	@BeanProperty var unset:Boolean = false
	//	
	//	def effectiveRating:JInteger = 
	//		if (unset) null
	//		else rating

	val maximumStars = 5

	def applyInternal() = transactional() {
		feedback.ratingHelpful = wasHelpful.toBoolean
		feedback.ratingPrompt = wasPrompt.toBoolean
	}

	def validate(errors: Errors) {
		if (enabled) {
			wasPrompt.update
			wasHelpful.update

			if (!wasPrompt.unset && wasPrompt.value == null)
				errors.rejectValue("wasPrompt", "feedback.rating.empty")
			if (!wasHelpful.unset && wasHelpful.value == null)
				errors.rejectValue("wasHelpful", "feedback.rating.empty")
		} else {
			errors.reject("feedback.rating.disabled")
		}
	}

	def enabled = features.collectRatings && feedback.collectRatings

	def describe(d: Description) = d.feedback(feedback).properties( //			"rating" -> effectiveRating,
	//			"previousRating" -> feedback.rating.orNull
	)
}