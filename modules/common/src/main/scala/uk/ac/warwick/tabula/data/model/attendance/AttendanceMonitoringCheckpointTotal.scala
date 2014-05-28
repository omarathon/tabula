package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.{Table, Column, JoinColumn, FetchType, ManyToOne, Entity}
import uk.ac.warwick.tabula.data.model.{Department, GeneratedId, StudentMember}
import javax.validation.constraints.NotNull
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import org.hibernate.annotations.Type

@Entity
@Table(name="ATTENDANCEMONITORINGTOTAL")
class AttendanceMonitoringCheckpointTotal extends GeneratedId {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	var student: StudentMember = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = _

	@NotNull
	@Column(name = "academicyear")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = _

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
