package uk.ac.warwick.tabula.commands.cm2.feedback

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{SelectedStudentsRequest, SelectedStudentsState}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.language.implicitConversions


object UnPublishFeedbackCommand {

	type Command = Appliable[Seq[Feedback]] with UnPublishFeedbackCommandRequest

	def apply(assignment: Assignment, submitter: CurrentUser) =
		new UnPublishFeedbackCommandInternal(assignment, submitter)
			with ComposableCommand[Seq[Feedback]]
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with UnPublishFeedbackPermissions
			with UnPublishFeedbackValidation
			with UnPublishFeedbackDescription
}

class UnPublishFeedbackCommandInternal(val assignment: Assignment, val submitter: CurrentUser) extends CommandInternal[Seq[Feedback]] with UnPublishFeedbackCommandRequest {
	self: UserLookupComponent with AutowiringFeedbackServiceComponent =>

	def applyInternal(): Seq[Feedback] = {
		feedbackToUnPublish.foreach(feedback => {
			feedback.released = false
			feedbackService.saveOrUpdate(feedback)
		})
		feedbackToUnPublish
	}
}


trait UnPublishFeedbackCommandState extends SelectedStudentsState {
	def assignment: Assignment
	def submitter: CurrentUser
}

trait UnPublishFeedbackCommandRequest extends SelectedStudentsRequest with UnPublishFeedbackCommandState {
	var confirm: Boolean = false
	def feedbackToUnPublish: Seq[Feedback] = if (students.isEmpty) Seq() else feedbacks.filter(_.released)
}

trait UnPublishFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: UnPublishFeedbackCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.UnPublish, mandatory(assignment))
	}
}

trait UnPublishFeedbackValidation extends SelfValidating {
	self: UnPublishFeedbackCommandRequest =>

	override def validate(errors: Errors) {
		if (feedbackToUnPublish.isEmpty) {
			errors.reject("feedback.unpublish.nofeedback")
		}
	}
}

trait UnPublishFeedbackDescription extends Describable[Seq[Feedback]] {
	self: UnPublishFeedbackCommandRequest with UserLookupComponent =>

	override lazy val eventName: String = "UnPublishFeedback"

	override def describe(d: Description) {
		val students = userLookup.getUsersByUserIds(feedbackToUnPublish.map(_.usercode)).values.toSeq
		d.assignment(assignment)
			.studentIds(students.flatMap(m => Option(m.getWarwickId)))
			.studentUsercodes(students.map(_.getUserId))
	}
}