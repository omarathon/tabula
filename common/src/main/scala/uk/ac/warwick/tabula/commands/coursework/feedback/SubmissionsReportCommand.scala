package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.commands.{Command, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._

import scala.collection.JavaConverters._

case class SubmissionsReport(assignment: Assignment) {
	private def feedbacks = assignment.fullFeedback
	private def submissions = assignment.submissions.asScala

	private val feedbackUsercodes = feedbacks.map(_.usercode).toSet
	private val submissionUsercodes = submissions.map(_.usercode).toSet

	// Subtract the sets from each other to obtain discrepancies
	val feedbackOnly: Set[String] = feedbackUsercodes &~ submissionUsercodes
	val submissionOnly: Set[String] = submissionUsercodes &~ feedbackUsercodes

	/**
		* We want to show a warning if some feedback items are missing either marks or attachments
		* If however, all feedback items have only marks or attachments then we don't send a warning.
		*/
	val withoutAttachments: Set[String] = feedbacks
		.filter(f => !f.hasAttachments && !f.comments.exists(_.hasText))
		.map(_.usercode).toSet
	val withoutMarks: Set[String] = feedbacks.filter(!_.hasMarkOrGrade).map(_.usercode).toSet
	val plagiarised: Set[String] = submissions.filter(_.suspectPlagiarised).map(_.usercode).toSet

	def hasProblems: Boolean = {
		val shouldBeEmpty = Set(feedbackOnly, submissionOnly, plagiarised)
		val problems = assignment.collectSubmissions && shouldBeEmpty.exists { _.nonEmpty }

		if (assignment.collectMarks) {
			val shouldBeEmptyWhenCollectingMarks = Set(withoutAttachments, withoutMarks)
			problems || shouldBeEmptyWhenCollectingMarks.exists { _.nonEmpty }
		} else {
			problems
		}
	}
}

class SubmissionReportCommand(val module: Module, val assignment: Assignment) extends Command[SubmissionsReport] with ReadOnly with Unaudited {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)

	def applyInternal(): SubmissionsReport = SubmissionsReport(assignment)

}
