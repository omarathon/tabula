package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import uk.ac.warwick.tabula.data.model.{GeneratedId, StudentCourseYearDetails, StudentCourseDetails, StudentMember}
import org.joda.time.DateTime
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.AcademicYear
import javax.validation.constraints.{Min, NotNull}

@Entity
class MonitoringPointReport extends GeneratedId {

	@NotNull
	var student: StudentMember = _

	@NotNull
	var studentCourseDetails: StudentCourseDetails = _

	@NotNull
	var studentCourseYearDetails: StudentCourseYearDetails = _

	@NotNull
	var createdDate: DateTime = _

	// CAN be null
	var pushedDate: DateTime = _

	@NotNull
	var monitoringPeriod: String = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime())

	@NotNull
	@Min(1)
	var missed: Int = _

	@NotNull
	var reporter: String = _
}