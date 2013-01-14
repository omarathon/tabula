package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{Description, SelfValidating, Command}
import uk.ac.warwick.tabula.data.Daoisms
import reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.StateService

class MarkingCompletedCommand(val assignment: Assignment, currentUser: CurrentUser, val firstMarker:Boolean )
	extends Command[Unit] with SelfValidating with Daoisms {

	var stateService = Wire.auto[StateService]

	@BeanProperty var students: JList[String] = ArrayList()
	@BeanProperty var confirm: Boolean = false

	def applyInternal() {

		for (
			uniId <- students;
			parentFeedback <- assignment.feedbacks.find(_.universityId == uniId)
		) {
			val markerFeedback = firstMarker match {
				case true => parentFeedback.retrieveFirstMarkerFeedback
				case false => parentFeedback.retrieveSecondMarkerFeedback
				case _ => throw throw new IllegalStateException("isFirstMarker must be true or false")
			}
			stateService.updateState(markerFeedback, MarkingCompleted)
		}

	}

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
	}

}
