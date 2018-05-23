package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet

import javax.sql.DataSource

import scala.collection.JavaConverters._
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportCourseDetailsNoteCommand
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseDetailNoteDaoComponent, StudentCourseDetailNoteDao}

trait StudentCourseDetailsNoteImporter {
	def getStudentCourseDetailsNotes: Seq[ImportCourseDetailsNoteCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class StudentCourseDetailsNoteImporterImpl extends StudentCourseDetailsNoteImporter with TaskBenchmarking {

	import StudentCourseDetailsNoteImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")
	var studentCourseDetailNoteDao: StudentCourseDetailNoteDao = Wire[StudentCourseDetailNoteDao]

	lazy val studentCourseDetailsNoteImporterQuery = new StudentCourseDetailsNoteQuery(sits)

	override def getStudentCourseDetailsNotes: Seq[ImportCourseDetailsNoteCommand] = {
		benchmarkTask("Fetch student course detail notes") {
			val allNotes = studentCourseDetailNoteDao.getAllNotes
			val foundNotes = studentCourseDetailsNoteImporterQuery.execute.asScala.map(r => new ImportCourseDetailsNoteCommand(r))
			val foundCodes = foundNotes.map(_.code)
			val deletedNotes = allNotes.filterNot(note => foundCodes.contains(note.code))
			deletedNotes.foreach(note => {
				logger.info(s"${note.code} is no longer in SITS - deleting note")
				studentCourseDetailNoteDao.delete(note)
			})
			foundNotes
		}
	}
}

@Profile(Array("sandbox"))
@Service
class SandboxStudentCourseDetailsNoteImporter extends StudentCourseDetailsNoteImporter {
	override def getStudentCourseDetailsNotes: Seq[ImportCourseDetailsNoteCommand] = Nil
}


object StudentCourseDetailsNoteImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	def NotesQuery =s"""select doc_code, doc_note
										  from $sitsSchema.men_doc where doc_dtyc in ('GRID_RECOMM', 'GRID_COMM', 'GRID_MEDNOTE')"""

	def mapResultSet(resultSet: ResultSet): StudentCourseDetailsNoteRow = {
		StudentCourseDetailsNoteRow(
			resultSet.getString("doc_code"),
			resultSet.getString("doc_note")
		)
	}

	class StudentCourseDetailsNoteQuery(ds: DataSource)
		extends MappingSqlQuery[StudentCourseDetailsNoteRow](ds, NotesQuery) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int): StudentCourseDetailsNoteRow = mapResultSet(resultSet)
	}
}

case class StudentCourseDetailsNoteRow(code: String, note: String)