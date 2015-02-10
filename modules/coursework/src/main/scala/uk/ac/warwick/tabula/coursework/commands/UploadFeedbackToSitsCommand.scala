package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UploadFeedbackToSitsCommand {
	def apply(module: Module, assignment: Assignment, currentUser: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new UploadFeedbackToSitsCommandInternal(module, assignment, currentUser, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringFeedbackForSitsServiceComponent
			with ComposableCommand[Seq[Feedback]]
			with UploadFeedbackToSitsDescription
			with UploadFeedbackToSitsPermissions
			with UploadFeedbackToSitsCommandState
}


class UploadFeedbackToSitsCommandInternal(val module: Module, val assignment: Assignment, currentUser: CurrentUser, gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Seq[Feedback]] {

	self: FeedbackServiceComponent with FeedbackForSitsServiceComponent =>

	lazy val gradeValidation = feedbackForSitsService.validateAndPopulateFeedback(assignment.fullFeedback, gradeGenerator)

	override def applyInternal() = {
		assignment.fullFeedback.flatMap(f => feedbackForSitsService.queueFeedback(f, currentUser, gradeGenerator)).map(_.feedback)
	}

}

trait UploadFeedbackToSitsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: UploadFeedbackToSitsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Feedback.Publish, assignment)
	}

}

trait UploadFeedbackToSitsDescription extends Describable[Seq[Feedback]] {

	self: UploadFeedbackToSitsCommandState =>

	override lazy val eventName = "UploadFeedbackToSits"

	override def describe(d: Description) {
		d.assignment(assignment)
	}
}

trait UploadFeedbackToSitsCommandState {
	def module: Module
	def assignment: Assignment
}
