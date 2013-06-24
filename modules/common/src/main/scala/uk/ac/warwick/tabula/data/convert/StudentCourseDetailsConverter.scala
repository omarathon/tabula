package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao

class StudentCourseDetailsConverter extends TwoWayConverter[String, StudentCourseDetails] {

	@Autowired var service: StudentCourseDetailsDao = _

	override def convertRight(scjCode: String) = {
		service.getByScjCode(scjCode).orNull
	}
	override def convertLeft(studentCourseDetails: StudentCourseDetails) = (Option(studentCourseDetails) map {_.scjCode}).orNull

}
