package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._
import org.hibernate.annotations.AccessType
import org.joda.time.DateTime
import javax.validation.constraints._
import uk.ac.warwick.tabula.data.model.GeneratedId
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState

@AccessType("field")
@Entity
@Table(uniqueConstraints = Array(
	new UniqueConstraint(columnNames = Array("occurrence_id", "universityId"))
))
class SmallGroupEventAttendance extends GeneratedId with PermissionsTarget with Serializable {
	
	@ManyToOne
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

	def permissionsParents = Stream(occurrence)

}