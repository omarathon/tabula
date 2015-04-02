package uk.ac.warwick.tabula.jobs.reports

import java.io.ByteArrayInputStream

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringZipServiceComponent}

import scala.collection.JavaConverters._

object ProfileExportJob {
	val identifier = "profile-export"
	val StudentKey = "students"
	val AcademicYearKey = "academicYear"
	val ZipFilePathKey = "zipFilePath"
	val BuildingZip = "Building .zip file"
	def status(universityId: String) = s"Exporting profile for $universityId"

	def apply(students: Seq[String], academicYear: AcademicYear) = JobPrototype(identifier, Map(
		StudentKey -> students.asJava,
		AcademicYearKey -> academicYear.toString
	))
}

@Component
class ProfileExportJob extends Job with AutowiringZipServiceComponent with AutowiringFileAttachmentServiceComponent {

	val identifier = ProfileExportJob.identifier

	// TODO remove this
	@Autowired var fileDao: FileDao = _

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			val students = job.getStrings(ProfileExportJob.StudentKey)

			updateProgress(0)

			val results: Map[String, Seq[FileAttachment]] = students.zipWithIndex.map{case(universityId, index) =>
				updateStatus(ProfileExportJob.status(universityId))

				// TAB-3428 - do something useful
				Thread.sleep(10000)
				val attachment = new FileAttachment
				attachment.setName(s"$universityId.txt")
				attachment.uploadedData = new ByteArrayInputStream(s"Profile for $universityId".getBytes)
				fileDao.saveTemporary(attachment)
				val result = (universityId, Seq(attachment))

				updateProgress(((index + 1).toFloat / students.size.toFloat * 100).toInt)
				result
			}.toMap

			updateProgress(100)

			updateStatus(ProfileExportJob.BuildingZip)

			val zipFile = zipService.getProfileExportZip(results)
			job.setString(ProfileExportJob.ZipFilePathKey, zipFile.getPath)
			
			job.succeeded = true
		}
	}
}
