package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringAssessmentMembershipServiceComponent, AssessmentMembershipServiceComponent, UserLookupComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

object FeedbackAdjustmentListCommand {
	def apply(assignment: Assignment) =
		new FeedbackAdjustmentListCommandInternal(assignment)
			with ComposableCommand[Seq[StudentInfo]]
			with FeedbackAdjustmentListCommandPermissions
			with AutowiringUserLookupComponent
			with AutowiringAssessmentMembershipServiceComponent
			with Unaudited
			with ReadOnly
}

case class StudentInfo(student: User, feedback: Option[Feedback])

class FeedbackAdjustmentListCommandInternal(val assignment: Assignment)
	extends CommandInternal[Seq[StudentInfo]] with FeedbackAdjustmentListCommandState {

	self: UserLookupComponent with AssessmentMembershipServiceComponent =>

	def applyInternal() = {
		val allFeedback = assignment.fullFeedback
		val allStudents =
			if (students.isEmpty) assessmentMembershipService.determineMembershipUsers(assignment)
			else students.asScala.map(userLookup.getUserByWarwickUniId)

		val (hasFeedback, noFeedback) = allStudents.map { student =>
			StudentInfo(student, allFeedback.find { _.universityId == student.getWarwickId })
		}.partition { _.feedback.isDefined }

		hasFeedback ++ noFeedback
	}
}

trait FeedbackAdjustmentListCommandState {
	def assignment: Assignment
	var students: JList[String] = JArrayList()
}

trait FeedbackAdjustmentListCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackAdjustmentListCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Feedback.Update, mandatory(assignment))
	}
}