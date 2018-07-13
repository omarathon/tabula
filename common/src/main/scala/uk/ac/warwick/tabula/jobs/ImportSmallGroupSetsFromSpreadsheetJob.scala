package uk.ac.warwick.tabula.jobs

import org.springframework.stereotype.Component
import org.springframework.validation.BeanPropertyBindingResult
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.commands.groups.admin.ImportSmallGroupSetsFromSpreadsheetCommand
import uk.ac.warwick.tabula.data.AutowiringFileDaoComponent
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment}
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.jobs.JobInstance
import scala.collection.JavaConverters._

object ImportSmallGroupSetsFromSpreadsheetJob {
	val identifier = "import-small-group-sets"

	def apply(department: Department, academicYear: AcademicYear, file: FileAttachment, locationMappings: Map[String, String]): JobPrototype = JobPrototype(identifier, Map(
		"department" -> department.id,
		"academicYear" -> academicYear.toString,
		"file" -> file.id,
		"locationMappings" -> locationMappings.asJava
	))
}

@Component
class ImportSmallGroupSetsFromSpreadsheetJob extends Job
	with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringFileDaoComponent {
	override val identifier: String = ImportSmallGroupSetsFromSpreadsheetJob.identifier

	override def run(implicit job: JobInstance): Unit = transactional() {
		val department = moduleAndDepartmentService.getDepartmentById(job.getString("department")).getOrElse(throw obsoleteJob)
		val academicYear = AcademicYear.parse(job.getString("academicYear"))

		val file = new UploadedFile
		file.attached = java.util.Arrays.asList(fileDao.getFileById(job.getString("file")).getOrElse(throw obsoleteJob))

		val command = ImportSmallGroupSetsFromSpreadsheetCommand(department, academicYear)
		command.file = file
		command.locationMappings = job.getStringMap("locationMappings").asJava

		updateStatus("Reading the spreadsheet")
		updateProgress(20)

		command.onBind(new BeanPropertyBindingResult(command, "command"))

		updateStatus("Applying changes")
		updateProgress(50)

		command.apply()

		updateStatus("Import complete")
		updateProgress(100)

		job.succeeded = true
	}
}
