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

	def this(student: StudentMember, department: Department, academicYear: AcademicYear) {
		this()
		this.student = student
		this.department = department
		this.academicYear = academicYear
	}

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
	var unrecorded: Int = 0

	@NotNull
	@Column(name = "authorized")
	var authorised: Int = 0

	@NotNull
	@Column(name = "unauthorized")
	var unauthorised: Int = 0

	@NotNull
	var attended: Int = 0

	@NotNull
	@Column(name = "updated_date")
	var updatedDate: DateTime = _

	def reset(): Unit = {
		unrecorded = 0
		authorised = 0
		unauthorised = 0
		attended = 0
	}

}
