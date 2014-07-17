package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._
import javax.persistence.CascadeType._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Type, Filter, FilterDef, AccessType, BatchSize}
import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.groups.SmallGroup._
import uk.ac.warwick.tabula.services.{SmallGroupMembershipHelpers, SmallGroupService, AssignmentMembershipService, UserGroupCacheManager}
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

object DepartmentSmallGroupSet {
	final val NotDeletedFilter = "notDeleted"
	object Settings {
		val DefaultMaxGroupSizeEnabled = "DefaultMaxGroupSizeEnabled"
		val DefaultMaxGroupSize = "DefaultMaxGroupSize"
	}

	// For sorting a collection by set name. Either pass to the sort function,
	// or expose as an implicit val.
	val NameOrdering = Ordering.by { set: DepartmentSmallGroupSet => (set.name, set.id) }

	// Companion object is one of the places searched for an implicit Ordering, so
	// this will be the default when ordering a list of small group sets.
	implicit val defaultOrdering = NameOrdering
}

/**
 * Represents a set of small groups with a Department. These are then linked to by an actual
 * SmallGroupSet rather than doing allocations manually.
 */
@FilterDef(name = DepartmentSmallGroupSet.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = DepartmentSmallGroupSet.NotDeletedFilter)
@Entity
@AccessType("field")
class DepartmentSmallGroupSet
	extends GeneratedId
	with CanBeDeleted
	with ToString
	with PermissionsTarget
	with HasSettings
	with Serializable
	with PostLoadBehaviour
	with ToEntityReference {
	type Entity = DepartmentSmallGroupSet

	import DepartmentSmallGroupSet._

	@transient var membershipService = Wire[AssignmentMembershipService]

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var smallGroupService = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

	def this(_department: Department) {
		this()
		this.department = _department
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)

	@NotNull
	var name: String = _

	var archived: JBoolean = false

	@Column(name="allocation_method")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethodUserType")
	var allocationMethod: SmallGroupAllocationMethod = _

	@Column(name="self_group_switching")
	var allowSelfGroupSwitching: Boolean = true

	@Column(name="signup_state")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpStateUserType")
	var signupState: SmallGroupSetSelfSignUpState = SmallGroupSetSelfSignUpState.Closed

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = _

	@OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval=true)
	@JoinColumn(name = "set_id")
	@BatchSize(size=200)
	var groups: JList[DepartmentSmallGroup] = JArrayList()

	// only students manually added or excluded. use allStudents to get all students in the group set
	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	private var _membersGroup = UserGroup.ofUniversityIds
	def members: UnspecifiedTypeUserGroup = {
		smallGroupService match {
			case Some(smallGroupService) => {
				new UserGroupCacheManager(_membersGroup, smallGroupService.departmentGroupSetManualMembersHelper)
			}
			case _ => _membersGroup
		}
	}
	def members_=(group: UserGroup) { _membersGroup = group }

	@Column(name = "member_query")
	var memberQuery: String = _

	def isStudentMember(user: User) = members.includesUser(user)

	def allStudents = members.users
	def allStudentsCount = members.size

	def unallocatedStudents = {
		val allocatedStudents = groups.asScala.flatMap { _.students.users }

		allStudents diff allocatedStudents
	}

	def unallocatedStudentsCount = {
		// TAB-2296 we can't rely just on counts here
		unallocatedStudents.size
	}

	def hasAllocated = groups.asScala.exists { !_.students.isEmpty }

	def permissionsParents = Option(department).toStream

	def defaultMaxGroupSizeEnabled = getBooleanSetting(Settings.DefaultMaxGroupSizeEnabled).getOrElse(false)
	def defaultMaxGroupSizeEnabled_=(isEnabled:Boolean) = settings += (Settings.DefaultMaxGroupSizeEnabled -> isEnabled)

	def defaultMaxGroupSize = getIntSetting(Settings.DefaultMaxGroupSize).getOrElse(DefaultGroupSize)
	def defaultMaxGroupSize_=(defaultSize:Int) = settings += (Settings.DefaultMaxGroupSize -> defaultSize)

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"department" -> department)

	def duplicateTo(department: Department, assessmentGroups: JList[AssessmentGroup] = JArrayList()): DepartmentSmallGroupSet = {
		val newSet = new DepartmentSmallGroupSet()
		newSet.id = id
		newSet.academicYear = academicYear
		newSet.allocationMethod = allocationMethod
		newSet.allowSelfGroupSwitching = allowSelfGroupSwitching
		newSet.archived = archived
		newSet.memberQuery = memberQuery
		newSet.groups = groups.asScala.map(_.duplicateTo(newSet)).asJava
		newSet._membersGroup = _membersGroup.duplicate()
		newSet.membershipService= membershipService
		newSet.department = department
		newSet.name = name
		newSet.signupState = signupState
		newSet.settings = Map() ++ settings
		newSet
	}

	def postLoad {
		ensureSettings
	}

	override def toEntityReference = new DepartmentSmallGroupSetEntityReference().put(this)

}
