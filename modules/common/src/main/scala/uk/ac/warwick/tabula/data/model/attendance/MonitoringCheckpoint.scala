package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.{Column, Entity, JoinColumn, ManyToOne}
import org.joda.time.DateTime

import javax.validation.constraints.NotNull
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import org.hibernate.annotations.Type
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.MonitoringPointService

@Entity
class MonitoringCheckpoint extends GeneratedId {

	@transient var monitoringPointService = Wire.auto[MonitoringPointService]

	@ManyToOne
	@JoinColumn(name = "point_id")
	var point: MonitoringPoint = _

	@ManyToOne
	@JoinColumn(name = "student_course_detail_id")
	var studentCourseDetail: StudentCourseDetails = _

	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceStateUserType")
	@Column(name = "state")
	private var _state: AttendanceState = _

	def state = _state
	def state_=(state: AttendanceState) {
		if (monitoringPointService.studentAlreadyReportedThisTerm(studentCourseDetail.student, point)){
			throw new IllegalArgumentException
		}
		_state = state
	}

	var updatedDate: DateTime = _

	@NotNull
	var updatedBy: String = _

	var autoCreated: Boolean = false

}