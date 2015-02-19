package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{UserLookupComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

object FeedbackAdjustmentListCommand {
	def apply(assignment: Assignment) =
		new FeedbackAdjustmentListCommandInternal(assignment)
			with ComposableCommand[Seq[StudentInfo]]
			with FeedbackAdjustmentListCommandPermissions
			with AutowiringUserLookupComponent
			with Unaudited
			with ReadOnly
}

case class StudentInfo(student: User, feedback: Option[Feedback])

class FeedbackAdjustmentListCommandInternal(val assignment: Assignment)
	extends CommandInternal[Seq[StudentInfo]] with FeedbackAdjustmentListCommandState {

	this: UserLookupComponent =>

	def applyInternal() = {
		val allFeedback = assignment.fullFeedback
		val allStudents =
			if (students.isEmpty) allFeedback.map { _.universityId }
			else students.asScala

		val (hasFeedback, noFeedback) = allStudents.map { universityId =>
			val student = userLookup.getUserByWarwickUniId(universityId)
			StudentInfo(student, allFeedback.find { _.universityId == universityId })
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