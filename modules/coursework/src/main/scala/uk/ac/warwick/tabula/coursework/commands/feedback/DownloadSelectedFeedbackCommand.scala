package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.zips.FeedbackZipFileJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.{AssessmentService, ZipService}
import uk.ac.warwick.tabula.services.fileserver.RenderableZip
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import scala.collection.JavaConverters._

/**
 * Download one or more submissions from an assignment, as a Zip.
 */
class DownloadSelectedFeedbackCommand(val module: Module, val assignment: Assignment, user: CurrentUser)
	extends Command[Either[RenderableZip, JobInstance]] with ReadOnly {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

	var assignmentService = Wire[AssessmentService]
	var zipService = Wire[ZipService]
	var feedbackDao = Wire[FeedbackDao]
	var jobService = Wire[JobService]


	var filename: String = _

	var students: JList[String] = JArrayList()

	var feedbacks: JList[AssignmentFeedback] = _

	override def applyInternal() = {
		if (students.isEmpty) throw new ItemNotFoundException

		feedbacks = (for (
			uniId <- students.asScala;
			feedback <- feedbackDao.getAssignmentFeedbackByUniId(assignment, uniId) // assignmentService.getSubmissionByUniId(assignment, uniId)
		) yield feedback).asJava


		if (feedbacks.asScala.exists(_.assignment != assignment)) {
				throw new IllegalStateException("Selected feedback doesn't match the assignment")
		}

		if (feedbacks.size() < FeedbackZipFileJob.minimumFeedbacks) {
			val zip = zipService.getSomeFeedbacksZip(feedbacks.asScala)
			val renderable = new RenderableZip(zip)
			Left(renderable)
		} else {
			Right(jobService.add(Option(user), FeedbackZipFileJob(feedbacks.asScala.map(_.id).toSeq)))
		}
	}

	override def describe(d: Description) = d
		.assignment(assignment)
		.studentIds(students.asScala)

	override def describeResult(d: Description) = d
		.assignment(assignment)
		.studentIds(students.asScala)
		.properties(
			"feedbackCount" -> Option(feedbacks).map(_.size).getOrElse(0))
}
