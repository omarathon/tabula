package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails

import uk.ac.warwick.tabula.system.TwoWayConverter

class StudentCourseYearDetailsIdConverter extends TwoWayConverter[String, StudentCourseYearDetails] with Daoisms {

	// print
	override def convertLeft(scyd: StudentCourseYearDetails) = Option(scyd) match {
		case Some(s) => s.id
		case None => null
	}

	// parse
	override def convertRight(id: String) = getById[StudentCourseYearDetails](id).orNull

}
