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
import uk.ac.warwick.tabula.actions.UploadMarkerFeedback

class MarkingCompletedCommand(val module: Module, val assignment: Assignment, currentUser: CurrentUser, val firstMarker:Boolean )
	extends Command[Unit] with SelfValidating with Daoisms {

	var stateService = Wire.auto[StateService]

	@BeanProperty var students: JList[String] = ArrayList()
	@BeanProperty var markerFeedback: JList[MarkerFeedback] = ArrayList()

	@BeanProperty var noMarks: JList[MarkerFeedback] = ArrayList()
	@BeanProperty var noFeedback: JList[MarkerFeedback] = ArrayList()

	@BeanProperty var confirm: Boolean = false

	mustBeLinked(assignment, module)
	PermissionsCheck(UploadMarkerFeedback(assignment))


	def onBind() {
		markerFeedback = students.flatMap(assignment.getMarkerFeedback(_, currentUser.apparentUser))
	}

	def applyInternal() {
		markerFeedback.foreach(stateService.updateState(_, MarkingCompleted))
	}

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("numFeedbackUpdated" -> markerFeedback.size())
	}

	def preSubmitValidation() {
		noMarks = markerFeedback.filter(!_.hasMarks)
		noFeedback = markerFeedback.filter(!_.hasFeedback)
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "markers.finishMarking.confirm")
	}

}
