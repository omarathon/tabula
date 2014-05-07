package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.{Column, JoinColumn, FetchType, ManyToOne, Entity}
import uk.ac.warwick.tabula.data.model.StudentMember
import javax.validation.constraints.NotNull
import org.joda.time.DateTime

@Entity
class AttendanceMonitoringCheckpointTotal {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	var student: StudentMember = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id")
	var scheme: AttendanceMonitoringScheme = _

	@NotNull
	var unrecorded: Int = _

	@NotNull
	var authorized: Int = _

	@NotNull
	var unauthorized: Int = _

	@NotNull
	var attended: Int = _

	@NotNull
	@Column(name = "updated_date")
	var updatedDate: DateTime = _

}
