package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.AcademicYear
import javax.validation.constraints.NotNull
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.model.{KnownTypeUserGroup, GeneratedId, Department, UserGroup}
import uk.ac.warwick.tabula.services.{AttendanceMonitoringMembershipHelpers, AttendanceMonitoringService, UserGroupCacheManager}
import uk.ac.warwick.spring.Wire
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.helpers.StringUtils._

@Entity
class AttendanceMonitoringScheme extends GeneratedId with PermissionsTarget with Serializable {

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var attendanceMonitoringService = Wire.option[AttendanceMonitoringService with AttendanceMonitoringMembershipHelpers]

	var name: String = _

	def displayName = {
		if (name.hasText)
			name
		else
		// TODO Could probably come up with something better here
			"Untitled scheme"
	}

	def shortDisplayName = displayName

	@NotNull
	@Column(name = "academicyear")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var academicYear: AcademicYear = _

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	private var _members: UserGroup = UserGroup.ofUniversityIds
	def members: KnownTypeUserGroup = {
		attendanceMonitoringService match {
			case Some(service) =>
				new UserGroupCacheManager(_members, service.membersHelper)
			case _ => _members
		}
	}
	def members_=(group: UserGroup) { _members = group }

	@Column(name = "member_query")
	var memberQuery: String = _


	@OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var points: JList[AttendanceMonitoringPoint] = JArrayList()

	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointStyleUserType")
	@Column(name = "point_style")
	var pointStyle: AttendanceMonitoringPointStyle = _

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = _

	@NotNull
	@Column(name = "created_date")
	var createdDate: DateTime = _

	@NotNull
	@Column(name = "updated_date")
	var updatedDate: DateTime = _

	def permissionsParents = Option(department).toStream

}


