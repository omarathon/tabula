package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{UserLookupComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object FeedbackAdjustmentListCommand {
	def apply(assignment: Assignment) =
		new FeedbackAdjustmentListCommandInternal(assignment)
			with ComposableCommand[Seq[StudentInfo]]
			with FeedbackAdjustmentListCommandPermissions
			with AutowiringUserLookupComponent
			with Unaudited
			with ReadOnly
}

case class StudentInfo(student: User, feedback: Feedback)

class FeedbackAdjustmentListCommandInternal(val assignment: Assignment)
	extends CommandInternal[Seq[StudentInfo]] with FeedbackAdjustmentListCommandState {

	this : UserLookupComponent =>

	def applyInternal() = {
		val unpubishedFeedback = assignment.fullFeedback
		unpubishedFeedback.map(f => {
			val student = userLookup.getUserByWarwickUniId(f.universityId)
			StudentInfo(student, f)
		})
	}
}

trait FeedbackAdjustmentListCommandState {
	val assignment: Assignment
}

trait FeedbackAdjustmentListCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackAdjustmentListCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Update, mandatory(assignment))
	}
}