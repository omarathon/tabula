package uk.ac.warwick.tabula.coursework.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assessment, Feedback, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UploadFeedbackToSitsCommand {
	def apply(module: Module, assessment: Assessment, currentUser: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
		new UploadFeedbackToSitsCommandInternal(module, assessment, currentUser, gradeGenerator)
			with AutowiringFeedbackServiceComponent
			with AutowiringFeedbackForSitsServiceComponent
			with ComposableCommand[Seq[Feedback]]
			with UploadFeedbackToSitsDescription
			with UploadFeedbackToSitsPermissions
			with UploadFeedbackToSitsCommandState
}


class UploadFeedbackToSitsCommandInternal(val module: Module, val assessment: Assessment, currentUser: CurrentUser, gradeGenerator: GeneratesGradesFromMarks)
	extends CommandInternal[Seq[Feedback]] {

	self: FeedbackServiceComponent with FeedbackForSitsServiceComponent =>

	lazy val gradeValidation = feedbackForSitsService.validateAndPopulateFeedback(assessment.fullFeedback, gradeGenerator)

	override def applyInternal() = {
		assessment.fullFeedback.flatMap(f => feedbackForSitsService.queueFeedback(f, currentUser, gradeGenerator)).map(_.feedback)
	}

}

trait UploadFeedbackToSitsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: UploadFeedbackToSitsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(mandatory(assessment), mandatory(module))
		p.PermissionCheck(Permissions.AssignmentFeedback.Publish, assessment)
	}

}

trait UploadFeedbackToSitsDescription extends Describable[Seq[Feedback]] {

	self: UploadFeedbackToSitsCommandState =>

	override lazy val eventName = "UploadFeedbackToSits"

	override def describe(d: Description) {
		d.assessment(assessment)
	}
}

trait UploadFeedbackToSitsCommandState {
	def module: Module
	def assessment: Assessment
}
