package uk.ac.warwick.tabula.data.model.groups

import org.hibernate.annotations.{AccessType, Filter, FilterDef, IndexColumn, Type}
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.ToString
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import javax.persistence._
import javax.persistence.FetchType._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.data.model.permissions.SmallGroupGrantedRole
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.tabula.data.model.groups.WeekRange._
import org.joda.time.LocalTime
import javax.validation.constraints.NotNull
import org.hibernate.annotations.BatchSize
@Entity
@AccessType("field")
class SmallGroupEvent extends GeneratedId with ToString with PermissionsTarget with Serializable {
	
	@transient var permissionsService = Wire[PermissionsService]

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var smallGroupService = Wire.option[SmallGroupService with SmallGroupMembershipHelpers]

	def this(_group: SmallGroup) {
		this()
		this.group = _group
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_id", insertable = false, updatable = false)
	var group: SmallGroup = _
	
	// Store Week Ranges as ACADEMIC week numbers - so week 1 is the first week of Autumn term, week 1 of spring term is 15 or 16, etc.
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.WeekRangeListUserType")
	var weekRanges: Seq[WeekRange] = Nil
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.DayOfWeekUserType")
	var day: DayOfWeek = _
	
	var startTime: LocalTime = _
	var endTime: LocalTime = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.LocationUserType")
	var location: Location = _
	
	var title: String = _
	
	def isUnscheduled = day == null || (startTime == null && endTime == null)
	def isSingleEvent = weekRanges.size == 1 && weekRanges.head.isSingleWeek
		
	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "tutorsgroup_id")
	private var _tutors: UserGroup = UserGroup.ofUsercodes
	def tutors: UnspecifiedTypeUserGroup = {
		smallGroupService match {
			case Some(smallGroupService) => {
				new UserGroupCacheManager(_tutors, smallGroupService.eventTutorsHelper)
			}
			case _ => _tutors
		}
	}
	def tutors_=(group: UserGroup) { _tutors = group }
	
	@OneToMany(mappedBy = "event", fetch = FetchType.LAZY, cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=50)
	var occurrences: JSet[SmallGroupEventOccurrence] = JHashSet()
	
	def permissionsParents = Option(group).toStream
	
	def toStringProps = Seq(
		"id" -> id,
		"weekRanges" -> weekRanges,
		"day" -> day,
		"startTime" -> startTime,
		"endTime" -> endTime)

  def isEquivalentTo(other: SmallGroupEvent):Boolean = {
    weekRanges == other.weekRanges &&
    day == other.day &&
    startTime == other.startTime &&
    endTime == other.endTime &&
    location == other.location &&
    tutors.hasSameMembersAs(other.tutors)
  }

  def duplicateTo(group:SmallGroup):SmallGroupEvent= {
    val newEvent = new SmallGroupEvent
    newEvent.id = id
    newEvent.day = day
    newEvent.endTime = endTime
    newEvent.group = group
    newEvent.location = location
    newEvent.permissionsService = permissionsService
    newEvent.startTime = startTime
    newEvent.title = title
    newEvent._tutors = _tutors.duplicate()
    newEvent.weekRanges = weekRanges

    newEvent
  }

}