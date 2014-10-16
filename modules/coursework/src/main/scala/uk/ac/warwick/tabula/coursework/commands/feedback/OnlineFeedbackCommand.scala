package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingState.{MarkingCompleted, Rejected}
import uk.ac.warwick.tabula.helpers.StringUtils._

object OnlineFeedbackCommand {
	def apply(module: Module, assignment: Assignment) =
		new OnlineFeedbackCommand(module, assignment)
			with ComposableCommand[Seq[StudentFeedbackGraph]]
			with OnlineFeedbackPermissions
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringAssignmentMembershipServiceComponent
			with Unaudited
			with ReadOnly
}

abstract class OnlineFeedbackCommand(val module: Module, val assignment: Assignment)
	extends CommandInternal[Seq[StudentFeedbackGraph]]
	with Appliable[Seq[StudentFeedbackGraph]] with OnlineFeedbackState {

	self: SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent with AssignmentMembershipServiceComponent =>

	def applyInternal() = {
		val studentsWithSubmissionOrFeedback = 
			userLookup.getUsersByWarwickUniIds(
				assignment.getUniIdsWithSubmissionOrFeedback.filter { _.hasText }.toSeq
			).values.filter { _.isFoundUser }.toSeq.sortBy { _.getWarwickId }

	  val studentsWithSubmissionOrFeedbackUniversityIds = studentsWithSubmissionOrFeedback.map(_.getWarwickId)

		val unsubmittedStudents = assignmentMembershipService.determineMembershipUsers(assignment)
				.filterNot { x => studentsWithSubmissionOrFeedbackUniversityIds.contains(x.getWarwickId) }

		val students = studentsWithSubmissionOrFeedback ++ unsubmittedStudents
		students.map { student =>
			val hasSubmission = submissionService.getSubmissionByUniId(assignment, student.getWarwickId).isDefined
			val feedback = feedbackService.getFeedbackByUniId(assignment, student.getWarwickId)
			val (hasFeedback, hasPublishedFeedback) = feedback match {
				case Some(f) => (true, f.released.booleanValue)
				case _ => (false, false)
			}
			new StudentFeedbackGraph(student, hasSubmission, hasFeedback, hasPublishedFeedback, false, false)
		}
	}

}


object OnlineMarkerFeedbackCommand {
	def apply(module: Module, assignment: Assignment, marker: User) =
		new OnlineMarkerFeedbackCommand(module, assignment, marker)
			with ComposableCommand[Seq[StudentFeedbackGraph]]
			with OnlineFeedbackPermissions
			with AutowiringUserLookupComponent
			with AutowiringSubmissionServiceComponent
			with AutowiringFeedbackServiceComponent
			with Unaudited
			with ReadOnly
}

abstract class OnlineMarkerFeedbackCommand(val module: Module, val assignment: Assignment, val marker: User)
	extends CommandInternal[Seq[StudentFeedbackGraph]]
	with Appliable[Seq[StudentFeedbackGraph]] with OnlineFeedbackState {

	self: SubmissionServiceComponent with FeedbackServiceComponent with UserLookupComponent =>

	def applyInternal() = {

		val submissions = Option(assignment.markingWorkflow) match {
			case Some(mw) => mw.getSubmissions(assignment, marker)
			case _ => Seq()
		}

		submissions.filter(_.isReleasedForMarking).map { submission =>			
			val student = userLookup.getUserByWarwickUniId(submission.universityId)
			val hasSubmission = true
			val feedback = feedbackService.getFeedbackByUniId(assignment, submission.universityId)
			// get all the feedbacks for this user and pick the most recent
			val markerFeedback = assignment.getAllMarkerFeedbacks(submission.universityId, marker).headOption

			val hasUncompletedFeedback = markerFeedback.exists(_.hasContent)
			// the current feedback for the marker is completed or if the parent feedback isn't a placeholder then marking is completed
			val hasCompletedFeedback = markerFeedback.exists(_.state == MarkingCompleted)
			val hasRejectedFeedback = markerFeedback.exists(_.state == Rejected)

			val hasPublishedFeedback = feedback match {
				case Some(f) => f.released.booleanValue
				case None => false
			}
			new StudentFeedbackGraph(student, hasSubmission, hasUncompletedFeedback, hasPublishedFeedback, hasCompletedFeedback, hasRejectedFeedback)
		}
	}
}


trait OnlineFeedbackPermissions extends RequiresPermissionsChecking {

	self: OnlineFeedbackState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.mustBeLinked(assignment, module)
		p.PermissionCheck(Permissions.Feedback.Read, assignment)
	}
}

trait OnlineFeedbackState {
	val assignment: Assignment
	val module: Module
}


case class StudentFeedbackGraph(
	student: User,
	hasSubmission: Boolean,
	hasUncompletedFeedback: Boolean,
	hasPublishedFeedback: Boolean,
	hasCompletedFeedback: Boolean,
	hasRejectedFeedback: Boolean
)