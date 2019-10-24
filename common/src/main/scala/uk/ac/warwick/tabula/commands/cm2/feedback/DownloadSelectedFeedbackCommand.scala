package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.zips.FeedbackZipFileJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import uk.ac.warwick.tabula.services.{AssessmentService, ZipService}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Download one or more submissions from an assignment, as a Zip.
  */
class DownloadSelectedFeedbackCommand(val assignment: Assignment, user: CurrentUser)
  extends Command[Either[RenderableFile, JobInstance]] with ReadOnly {

  PermissionCheck(Permissions.AssignmentFeedback.Read, assignment)

  var assignmentService: AssessmentService = Wire[AssessmentService]
  var zipService: ZipService = Wire[ZipService]
  var feedbackDao: FeedbackDao = Wire[FeedbackDao]
  var jobService: JobService = Wire[JobService]


  var filename: String = _

  var students: JList[String] = JArrayList()

  var feedbacks: JList[AssignmentFeedback] = _

  override def applyInternal(): Either[RenderableFile, JobInstance] = {
    if (students.isEmpty) throw new ItemNotFoundException

    feedbacks = students.asScala
      .flatMap(feedbackDao.getAssignmentFeedbackByUsercode(assignment, _))
      .filter(f => !assignment.hasCM2Workflow || f.isMarkingCompleted)
      .asJava

    if (feedbacks.asScala.exists(_.assignment != assignment)) {
      throw new IllegalStateException("Selected feedback doesn't match the assignment")
    }

    if (feedbacks.size() < FeedbackZipFileJob.minimumFeedbacks) {
      val zip = Await.result(zipService.getSomeFeedbacksZip(feedbacks.asScala), Duration.Inf)
      Left(zip)
    } else {
      Right(jobService.add(Option(user), FeedbackZipFileJob(feedbacks.asScala.map(_.id))))
    }
  }

  override def describe(d: Description): Unit = d
    .assignment(assignment)
    .studentUsercodes(students.asScala)

  override def describeResult(d: Description): Unit =
    d.assignment(assignment)
     .feedbacks(feedbacks.asScala)
}
