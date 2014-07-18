package uk.ac.warwick.tabula.data.model.groups

import javax.persistence.CascadeType._
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{AccessType, Filter, FilterDef}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{SmallGroupMembershipHelpers, SmallGroupService, UserGroupCacheManager}

object DepartmentSmallGroup {
	final val NotDeletedFilter = "notDeleted"

	// For sorting a collection by group name. Either pass to the sort function,
	// or expose as an implicit val.
	val NameOrdering = Ordering.by { group: DepartmentSmallGroup => (group.name, group.id) }

	// Companion object is one of the places searched for an implicit Ordering, so
	// this will be the default when ordering a list of small groups.
	implicit val defaultOrdering = NameOrdering
}

@FilterDef(name = SmallGroup.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = SmallGroup.NotDeletedFilter)
@Entity
@AccessType("field")
class DepartmentSmallGroup
	extends GeneratedId
	with CanBeDeleted
	with ToString
	with PermissionsTarget
	with Serializable
	with ToEntityReference {
	type Entity = DepartmentSmallGroup
	import DepartmentSmallGroup._

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var smallGroupService = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

	def this(_set: DepartmentSmallGroupSet) {
		this()
		this.groupSet = _set
	}

	@NotNull
	var name: String = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "set_id", insertable = false, updatable = false)
	var groupSet: DepartmentSmallGroupSet = _

	def permissionsParents = Option(groupSet).toStream
	override def humanReadableId = name

	/**
	 * Direct access to the underlying UserGroup. Most of the time you don't want to us this; unless you're setting
	 * it to a new UserGroup, you should probably access "students" instead and work with Users rather than guessing what
	 * the right sort of UserId to use is.
	 */
	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "studentsgroup_id")
	private var _studentsGroup: UserGroup = UserGroup.ofUniversityIds
	def students: UnspecifiedTypeUserGroup = {
		smallGroupService match {
			case Some(smallGroupService) => {
				new UserGroupCacheManager(_studentsGroup, smallGroupService.departmentStudentGroupHelper)
			}
			case _ => _studentsGroup
		}
	}
	def students_=(group: UserGroup) { _studentsGroup = group }

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"set" -> groupSet)

	def duplicateTo(groupSet: DepartmentSmallGroupSet): DepartmentSmallGroup = {
		val newGroup = new DepartmentSmallGroup()
		newGroup.id = id
		newGroup.groupSet = groupSet
		newGroup.name = name
		newGroup._studentsGroup = _studentsGroup.duplicate()
		newGroup
	}

	override def toEntityReference = new DepartmentSmallGroupEntityReference().put(this)

}
