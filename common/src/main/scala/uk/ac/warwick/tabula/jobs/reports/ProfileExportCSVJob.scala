package uk.ac.warwick.tabula.jobs.reports

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.reports.profiles.ProfileExportSingleCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember}
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringProfileServiceComponent, AutowiringZipServiceComponent}

import scala.collection.JavaConverters._


object ProfileExportCSVJob {
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
class ProfileExportCSVJob extends Job with AutowiringZipServiceComponent
	with AutowiringFileAttachmentServiceComponent with AutowiringProfileServiceComponent {

	val identifier: String = ProfileExportCSVJob.identifier

	override def run(implicit job: JobInstance): Unit = new Runner(job).run()

	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		def run(): Unit = {
			transactional() {
				val studentIDs = job.getStrings(ProfileExportCSVJob.StudentKey)
				val students = profileService.getAllMembersWithUniversityIds(studentIDs).flatMap{
					case student: StudentMember => Some(student)
					case _ => None
				}
				val academicYear = AcademicYear.parse(job.getString(ProfileExportCSVJob.AcademicYearKey))

				updateProgress(0)

				val results: Map[String, Seq[FileAttachment]] = students.zipWithIndex.map{case(student, index) =>
					updateStatus(ProfileExportCSVJob.status(student.universityId))

					val result: Seq[FileAttachment] = ProfileExportSingleCommand(student, academicYear, job.user).apply()

					updateProgress(index + 1, students.size)

					student.universityId -> result
				}.toMap

				updateProgress(100)

				updateStatus(ProfileExportCSVJob.BuildingZip)

				val zipFile = zipService.getProfileExportZip(results)
				job.setString(ProfileExportCSVJob.ZipFilePathKey, zipFile.filename)

				job.succeeded = true
			}
		}
	}
}
