package uk.ac.warwick.tabula.jobs.reports

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import collection.JavaConverters._

object ProfileExportJob {
	val identifier = "profile-export"
	val StudentKey = "students"
	val AcademicYearKey = "academicYear"
	val ZipIdKey = "zipId"
	def status(universityId: String) = s"Exporting profile for $universityId"

	def apply(students: Seq[String], academicYear: AcademicYear) = JobPrototype(identifier, Map(
		StudentKey -> students.asJava,
		AcademicYearKey -> academicYear.toString
	))
}

@Component
class ProfileExportJob extends Job {

	val identifier = ProfileExportJob.identifier

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			val students = job.getStrings(ProfileExportJob.StudentKey)

			updateProgress(0)

			val results = students.zipWithIndex.map{case(universityId, index) =>
				updateStatus(ProfileExportJob.status(universityId))
				// TAB-3428 - do something useful
				Thread.sleep(10000)
				val result = universityId
				updateProgress(((index + 1).toFloat / students.size.toFloat * 100).toInt)
				result
			}

			updateProgress(100)
			job.succeeded = true
		}
	}
}
