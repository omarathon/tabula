package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentCourseDetailsNote


trait StudentCourseDetailNoteDaoComponent {
	val studentCourseDetailNoteDao: StudentCourseDetailNoteDao
}

trait AutowiringStudentCourseDetailNoteDaoComponent extends StudentCourseDetailNoteDaoComponent {
	val studentCourseDetailNoteDao: StudentCourseDetailNoteDao = Wire[StudentCourseDetailNoteDao]
}

trait StudentCourseDetailNoteDao {
	def saveOrUpdate(note: StudentCourseDetailsNote)
	def getNoteByCode(code: String): Option[StudentCourseDetailsNote]
	def getAllNotes: Seq[StudentCourseDetailsNote]
}

@Repository
class StudentCourseDetailNoteDaoImpl extends StudentCourseDetailNoteDao with Daoisms {

	override def saveOrUpdate(note: StudentCourseDetailsNote): Unit = session.saveOrUpdate(note)

	override def getNoteByCode(code: String): Option[StudentCourseDetailsNote] = session.newCriteria[StudentCourseDetailsNote].add(is("code", code)).uniqueResult

	override def getAllNotes: Seq[StudentCourseDetailsNote] = session.newQuery[StudentCourseDetailsNote]("from StudentCourseDetailNote").seq
}

