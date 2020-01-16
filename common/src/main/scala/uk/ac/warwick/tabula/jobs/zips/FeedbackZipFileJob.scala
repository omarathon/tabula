package uk.ac.warwick.tabula.jobs.zips

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, AutowiringZipServiceComponent}

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object FeedbackZipFileJob {
  val identifier = "feedback-zip-file"
  val zipFileName = "feedback.zip"
  val minimumFeedbacks = 10
  val FeedbacksKey = "feedback"

  def apply(feedbacks: Seq[String]) = JobPrototype(identifier, Map(
    FeedbacksKey -> feedbacks.asJava
  ))
}

@Component
class FeedbackZipFileJob extends ZipFileJob with AutowiringZipServiceComponent with AutowiringFeedbackServiceComponent with Daoisms {

  override val identifier: String = FeedbackZipFileJob.identifier
  override val zipFileName: String = FeedbackZipFileJob.zipFileName
  override val itemDescription = "feedback"

  override def run(implicit job: JobInstance): Unit = new Runner(job).run()

  class Runner(job: JobInstance) {
    def run(): Unit = {
      transactional() {
        val feedbacks = job.getStrings(FeedbackZipFileJob.FeedbacksKey).flatMap(feedbackService.getFeedbackById)

        updateProgress(0)(job)
        updateStatus("Initialising")(job)

        session.detach(job)

        val zipFile = Await.result(zipService.getSomeFeedbacksZip(feedbacks, updateZipProgress(_, _) {
          session.refresh(job)
          job
        }), Duration.Inf)

        session.refresh(job)
        job.setString(ZipFileJob.ZipFilePathKey, zipFile.filename)

        updateProgress(100)(job)
        job.succeeded = true
      }
    }
  }

}
