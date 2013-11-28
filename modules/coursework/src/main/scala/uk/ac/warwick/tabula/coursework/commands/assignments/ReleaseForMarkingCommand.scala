package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import scala.collection.JavaConverters._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.coursework.commands.markingworkflows.notifications.{ReleasedState, FeedbackReleasedNotifier}
import uk.ac.warwick.tabula.helpers.Logging

object ReleaseForMarkingCommand {
	def apply(module: Module, assignment: Assignment, user: User) =
		new ReleaseForMarkingCommand(module, assignment, user)
			with ComposableCommand[List[Feedback]]
			with ReleaseForMarkingCommandPermissions
			with ReleaseForMarkingCommandDescription
			with FirstMarkerReleaseNotifier
			with AutowiringAssignmentServiceComponent
			with AutowiringStateServiceComponent
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
}


abstract class ReleaseForMarkingCommand(val module: Module, val assignment: Assignment, val user: User)
	extends CommandInternal[List[Feedback]] with Appliable[List[Feedback]] with ReleaseForMarkingState with ReleasedState
	with SelfValidating with UserAware {

	this: AssignmentServiceComponent with StateServiceComponent with FeedbackServiceComponent =>

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
				newFeedback.uploaderId = user.getUserId
				newFeedback.universityId = uniId
				newFeedback.released = false
				feedbackService.saveOrUpdate(newFeedback)
				newFeedback
			})
			parentFeedback
		}

		val feedbackToUpdate:Seq[Feedback] = feedbacks -- invalidFeedback

		newReleasedFeedback = feedbackToUpdate map(f => {
			val markerFeedback = f.retrieveFirstMarkerFeedback
			stateService.updateState(markerFeedback, MarkingState.ReleasedForMarking)
			markerFeedback
		})

		feedbacksUpdated = feedbackToUpdate.size
		feedbackToUpdate.toList
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

trait ReleaseForMarkingState {
	import uk.ac.warwick.tabula.JavaImports._

	val assignment: Assignment
	val module: Module

	var students: JList[String] = JArrayList()
	var confirm: Boolean = false
	var invalidFeedback: JList[Feedback] = JArrayList()

	var feedbacksUpdated = 0
}

trait ReleaseForMarkingCommandPermissions extends RequiresPermissionsChecking {
	self: ReleaseForMarkingState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Submission.ReleaseForMarking, assignment)
	}
}

trait ReleaseForMarkingCommandDescription extends Describable[List[Feedback]] {

	self: ReleaseForMarkingState =>

	override def describe(d: Description){
		d.assignment(assignment)
			.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
			.property("submissionCount" -> feedbacksUpdated)
	}
}

trait FirstMarkerReleaseNotifier extends FeedbackReleasedNotifier[List[Feedback]] {
	this: ReleaseForMarkingState with ReleasedState with UserAware with UserLookupComponent with Logging =>
	def isFirstMarker = true
}