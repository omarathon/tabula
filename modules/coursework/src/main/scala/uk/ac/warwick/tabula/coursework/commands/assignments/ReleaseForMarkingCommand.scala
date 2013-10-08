package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{FeedbackService, StateService, AssignmentService}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.CurrentUser
import scala.collection.JavaConverters._

class ReleaseForMarkingCommand(val module: Module, val assignment: Assignment, currentUser: CurrentUser) 
	extends Command[List[Feedback]] with SelfValidating {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.ReleaseForMarking, assignment)

	var assignmentService = Wire.auto[AssignmentService]
	var stateService = Wire.auto[StateService]
	var feedbackService = Wire[FeedbackService]

	var students: JList[String] = JArrayList()
	var confirm: Boolean = false
	var invalidFeedback: JList[Feedback] = JArrayList()

	var feedbacksUpdated = 0

	// we must go via the marking workflow directly to determine if the student has a marker - not all workflows use the markerMap on assignment
	def studentsWithKnownMarkers:Seq[String] = students.filter(assignment.markingWorkflow.studentHasMarker(assignment, _))
	def unreleasableSubmissions:Seq[String] = (studentsWithoutKnownMarkers ++ studentsAlreadyReleased).distinct

	def studentsWithoutKnownMarkers:Seq[String] = students -- studentsWithKnownMarkers
	def studentsAlreadyReleased = invalidFeedback.asScala.map(f=>f.universityId)

	def applyInternal() = {
		// get the parent feedback or create one if none exist
		val feedbacks = studentsWithKnownMarkers.toBuffer.map{ uniId:String =>
			val parentFeedback = assignment.feedbacks.find(_.universityId == uniId).getOrElse({
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = currentUser.apparentId
				newFeedback.universityId = uniId
				newFeedback.released = false
				feedbackService.saveOrUpdate(newFeedback)
				newFeedback
			})
			parentFeedback
		}

		val feedbackToUpdate:Seq[Feedback] = feedbacks -- invalidFeedback
		feedbackToUpdate foreach (f => stateService.updateState(f.retrieveFirstMarkerFeedback, MarkingState.ReleasedForMarking))
		feedbacksUpdated = feedbackToUpdate.size
		feedbackToUpdate.toList
	}

	override def describe(d: Description){
		d.assignment(assignment)
		.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
		.property("submissionCount" -> feedbacksUpdated)
	}

	def preSubmitValidation() {
		invalidFeedback = for {
			universityId <- students
			parentFeedback <- assignment.feedbacks.find(_.universityId == universityId)
			if parentFeedback.firstMarkerFeedback != null
		} yield parentFeedback
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.release.for.marking.confirm")
	}

}

object ReleaseForMarkingCommand{
	def apply(module: Module, assignment: Assignment, currentUser: CurrentUser):ReleaseForMarkingCommand = {
		new ReleaseForMarkingCommand(module,assignment, currentUser) 
	}
}
