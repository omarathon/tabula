package uk.ac.warwick.tabula.jobs.zips

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, AutowiringZipServiceComponent}

import scala.jdk.CollectionConverters._
import uk.ac.warwick.tabula.data.Transactions._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object SubmissionZipFileJob {
  val identifier = "submission-zip-file"
  val zipFileName = "submissions.zip"
  val minimumSubmissions = 10
  val SubmissionsKey = "submissions"

  def apply(submissions: Seq[String]) = JobPrototype(identifier, Map(
    SubmissionsKey -> submissions.asJava
  ))
}

@Component
class SubmissionZipFileJob extends ZipFileJob with AutowiringZipServiceComponent with AutowiringSubmissionServiceComponent with Daoisms {

  override val identifier: String = SubmissionZipFileJob.identifier
  override val zipFileName: String = SubmissionZipFileJob.zipFileName
  override val itemDescription = "submissions"

  override def run(implicit job: JobInstance): Unit = new Runner(job).run()

  class Runner(job: JobInstance) {
    def run(): Unit = {
      transactional() {
        val submissions = job.getStrings(SubmissionZipFileJob.SubmissionsKey).flatMap(submissionService.getSubmission)

        updateProgress(0)(job)
        updateStatus("Initialising")(job)

        session.detach(job)

        val zipFile = Await.result(zipService.getSomeSubmissionsZip(submissions, updateZipProgress(_, _) {
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
