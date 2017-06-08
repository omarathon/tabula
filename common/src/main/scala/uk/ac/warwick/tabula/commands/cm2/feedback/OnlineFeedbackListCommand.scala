package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, Submission}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

case class EnhancedFeedback(
	student: User,
	submission: Option[Submission],
	feedback: Option[Feedback]
) {
	def hasSubmission:Boolean = submission.isDefined
	def isPublished:Boolean = feedback.exists(_.released)
	def hasContent:Boolean = feedback.exists(_.hasContent)
}

object OnlineFeedbackListCommand {
	def apply(assignment: Assignment) = new OnlineFeedbackListCommandInternal(assignment)
		with ComposableCommand[Seq[EnhancedFeedback]]
		with OnlineFeedbackListPermissions
		with AutowiringAssessmentMembershipServiceComponent
		with Unaudited with ReadOnly
}

class OnlineFeedbackListCommandInternal(val assignment: Assignment) extends CommandInternal[Seq[EnhancedFeedback]] with OnlineFeedbackListState {

	self: AssessmentMembershipServiceComponent  =>

	def applyInternal(): Seq[EnhancedFeedback] = {
		assessmentMembershipService.determineMembership(assignment)
			.items
			.map(_.user)
			.map(u => {
				val submission = assignment.submissions.asScala.find(_.usercode == u.getUserId)
				val feedback = assignment.allFeedback.find(_.usercode == u.getUserId)
				EnhancedFeedback(u, submission, feedback)
			})
		.sortBy(ef => Option(ef.student.getWarwickId).getOrElse(ef.student.getUserId))
	}
}

trait OnlineFeedbackListPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: OnlineFeedbackListState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
	}
}

trait OnlineFeedbackListState {
	val assignment: Assignment
}