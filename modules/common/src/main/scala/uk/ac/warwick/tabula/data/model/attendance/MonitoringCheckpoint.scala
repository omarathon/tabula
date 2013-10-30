package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.Entity
import org.joda.time.DateTime

import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import org.hibernate.annotations.Type

@Entity
class MonitoringCheckpoint extends GeneratedId {
	
	@ManyToOne
	@JoinColumn(name = "point_id")
	var point: MonitoringPoint = _
	
	@ManyToOne
	@JoinColumn(name = "student_course_detail_id")
	var studentCourseDetail: StudentCourseDetails = _
	
	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpointStateUserType")
	var state: MonitoringCheckpointState = _
	
	var updatedDate: DateTime = _
	
	@NotNull
	var updatedBy: String = _

	var autoCreated: Boolean = false

}