package uk.ac.warwick.tabula.jobs.scheduling

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportProfilesCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import uk.ac.warwick.tabula.services.jobs.JobInstance

import scala.collection.JavaConverters._

object ImportMembersJob {
	val identifier = "import-members"
	val MembersKey = "members"
	val YearsKey = "yearsToImport"

	def status(universityId: String) = s"Importing $universityId"

	def apply(universityIds: Seq[String], yearsToImport: Seq[AcademicYear]) = JobPrototype(identifier, Map(
		MembersKey -> universityIds.asJava,
		YearsKey -> yearsToImport.map(_.startYear.toString).asJava
	))
}

@Component
class ImportMembersJob extends Job {

	val identifier: String = ImportMembersJob.identifier

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			val memberIds = job.getStrings(ImportMembersJob.MembersKey)
			val yearsToImport = job.getStrings(ImportMembersJob.YearsKey).map(AcademicYear.parse)

			updateProgress(0)

			memberIds.zipWithIndex.foreach{case (universityId, index) =>
				updateStatus(ImportMembersJob.status(universityId))

				transactional() {
					val command = new ImportProfilesCommand
					command.componentMarkYears = yearsToImport
					command.refresh(universityId, None)
				}

				updateProgress(index + 1, memberIds.size)
			}

			updateStatus("Import complete")
			updateProgress(100)

			job.succeeded = true
		}
	}
}
