package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.Entity
import org.joda.time.DateTime

import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.data.model.StudentCourseDetails

@Entity
class MonitoringCheckpoint extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_id")
	var point: MonitoringPoint = _
	
	@ManyToOne
	@JoinColumn(name = "student_course_detail_id")
	var studentCourseDetail: StudentCourseDetails = _
	
	var checked: Boolean = false
	
	var createdDate: DateTime = _
	
	@NotNull
	var createdBy: String = _

}