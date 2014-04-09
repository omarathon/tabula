package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails}
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDao
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.spring.Wire

class StudentCourseYearDetailsIdConverter extends TwoWayConverter[String, StudentCourseYearDetails]  {
	var dao = Wire.auto[StudentCourseYearDetailsDao]

	// print
	override def convertLeft(scyd: StudentCourseYearDetails) = Option(scyd) match {
		case Some(s) => s.id
		case None => null
	}

	// parse
	override def convertRight(id: String) = dao.getStudentCourseYearDetails(id).orNull
}
