package uk.ac.warwick.tabula.data.model.groups

import org.hibernate.annotations.{Filter, FilterDef, BatchSize, AccessType}
import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import javax.validation.constraints.NotNull
import scala.collection.JavaConverters._

object SmallGroup {
	final val NotDeletedFilter = "notDeleted"
	final val DefaultGroupSize = 15
	object Settings {
		val MaxGroupSize = "MaxGroupSize"
	}
	
	// For sorting a collection by group name. Either pass to the sort function,
	// or expose as an implicit val.
	val NameOrdering = Ordering.by { group: SmallGroup => (group.name, group.id) }

	// Companion object is one of the places searched for an implicit Ordering, so
	// this will be the default when ordering a list of small groups.
	implicit val defaultOrdering = NameOrdering
}

/**
 * Represents a single small teaching group within a group set.
 */
@FilterDef(name = SmallGroup.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = SmallGroup.NotDeletedFilter)
@Entity
@AccessType("field")
class SmallGroup extends GeneratedId with CanBeDeleted with ToString with PermissionsTarget with HasSettings with Serializable with PostLoadBehaviour {
	import SmallGroup._
	
	@transient var permissionsService = Wire[PermissionsService]

	def this(_set: SmallGroupSet) {
		this()
		this.groupSet = _set
	}

	@NotNull
	var name: String = _

	@ManyToOne
	@JoinColumn(name = "set_id", insertable = false, updatable = false)
	var groupSet: SmallGroupSet = _
	
	@OneToMany(fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval=true)
	@JoinColumn(name = "group_id")
	@BatchSize(size=200)
	var events: JList[SmallGroupEvent] = JArrayList()
	
	def permissionsParents = Option(groupSet).toStream

	/**
	 * Direct access to the underlying UserGroup. Most of the time you don't want to us this; unless you're setting
	 * it to a new UserGroup, you should probably access "students" instead and work with Users rather than guessing what
	 * the right sort of UserId to use is.
	 */
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "studentsgroup_id")
	var _studentsGroup: UserGroup = UserGroup.ofUniversityIds
  def students: UnspecifiedTypeUserGroup = _studentsGroup

	def maxGroupSize = getIntSetting(Settings.MaxGroupSize)
	def maxGroupSize_=(defaultSize:Int) = settings += (Settings.MaxGroupSize -> defaultSize)

	def isFull = groupSet.defaultMaxGroupSizeEnabled && maxGroupSize.getOrElse(0) <= students.size

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"set" -> groupSet)


  def hasEquivalentEventsTo(other:SmallGroup) = {
    (this eq other ) ||
    {
      val eventsSC = events.asScala
      val otherEvents = other.events.asScala
      val allMyEventsExistOnOther = eventsSC.forall(ev=>otherEvents.exists(oe=>oe.isEquivalentTo(ev)))
      val allOthersEventsExistOnMe = otherEvents.forall(oe=>eventsSC.exists(ev=>ev.isEquivalentTo(oe)))
      allMyEventsExistOnOther && allOthersEventsExistOnMe
    }
  }

  def duplicateTo( groupSet:SmallGroupSet):SmallGroup = {
    val newGroup=new SmallGroup()
    newGroup.id = id
    newGroup.events = events.asScala.map(_.duplicateTo(newGroup)).asJava
    newGroup.groupSet = groupSet
    newGroup.name = name
    newGroup.permissionsService = permissionsService
    newGroup._studentsGroup = _studentsGroup.duplicate()
    newGroup.settings = Map() ++ settings
    newGroup
  }

	def postLoad {
		ensureSettings
	}
	
	def hasScheduledEvents = events.asScala.exists(!_.isUnscheduled)

}