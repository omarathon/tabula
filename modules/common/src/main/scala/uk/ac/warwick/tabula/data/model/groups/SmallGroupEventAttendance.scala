package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._
import org.hibernate.annotations.{BatchSize, AccessType, Type}
import org.joda.time.DateTime
import javax.validation.constraints._
import uk.ac.warwick.tabula.data.model.GeneratedId
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.JavaImports._

@AccessType("field")
@Entity
@Table(uniqueConstraints = Array(
	new UniqueConstraint(columnNames = Array("occurrence_id", "universityId"))
))
class SmallGroupEventAttendance extends GeneratedId with PermissionsTarget with Serializable {
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="occurrence_id")
	var occurrence: SmallGroupEventOccurrence = _
	
	@NotNull
	var universityId: String = _
	
	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceStateUserType")
	var state: AttendanceState = _
	
	var updatedDate: DateTime = _
	
	@NotNull
	var updatedBy: String = _

	@Column(name = "added_manually")
	var addedManually: Boolean = false

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="replaces_attendance_id")
	var replacesAttendance: SmallGroupEventAttendance = _

	@OneToMany(mappedBy = "replacesAttendance")
	@BatchSize(size=5)
	var replacedBy: JSet[SmallGroupEventAttendance] = JHashSet()

	def permissionsParents = Stream(occurrence)

}