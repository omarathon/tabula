package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.AcademicYear
import javax.validation.constraints.NotNull
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.model.{GeneratedId, Department, UnspecifiedTypeUserGroup, UserGroup}
import uk.ac.warwick.tabula.services.{AttendanceMonitoringMembershipHelpers, AttendanceMonitoringService, UserGroupCacheManager}
import uk.ac.warwick.spring.Wire
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.tabula.JavaImports._
import scala.Some
import org.joda.time.DateTime
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Entity
class AttendanceMonitoringScheme extends GeneratedId with PermissionsTarget with Serializable {

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var attendanceMonitoringService = Wire.option[AttendanceMonitoringService with AttendanceMonitoringMembershipHelpers]

	var name: String = _

	@NotNull
	@Column(name = "academicyear")
	var academicYear: AcademicYear = _

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	private var _members: UserGroup = UserGroup.ofUniversityIds
	def members: UnspecifiedTypeUserGroup = {
		attendanceMonitoringService match {
			case Some(service) =>
				new UserGroupCacheManager(_members, service.membersHelper)
			case _ => _members
		}
	}
	def members_=(group: UserGroup) { _members = group }

	@Column(name = "member_query")
	var memberQuery: String = _

	@OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval=true)
	@JoinColumn(name = "scheme_id")
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


