package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.spring.Wire

class StudentCourseDetailsConverter extends TwoWayConverter[String, StudentCourseDetails] {

	var service = Wire.auto[StudentCourseDetailsDao]

	override def convertRight(scjCode: String) = {
		val scjCodeDecoded = scjCode.replace("_","/")
		service.getByScjCode(scjCodeDecoded).orNull
	}
	override def convertLeft(studentCourseDetails: StudentCourseDetails) = (Option(studentCourseDetails) map {_.scjCode}).orNull

}
