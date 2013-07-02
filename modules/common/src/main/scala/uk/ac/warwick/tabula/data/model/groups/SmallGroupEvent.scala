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
@Entity
@AccessType("field")
class SmallGroupEvent extends GeneratedId with ToString with PermissionsTarget with Serializable {
	
	@transient var permissionsService = Wire[PermissionsService]

	def this(_group: SmallGroup) {
		this()
		this.group = _group
	}
	
	@ManyToOne
	@JoinColumn(name = "group_id", insertable = false, updatable = false)
	var group: SmallGroup = _
	
	// Store Week Ranges as ACADEMIC week numbers - so week 1 is the first week of Autumn term, week 1 of spring term is 15 or 16, etc.
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.WeekRangeListUserType")
	var weekRanges: Seq[WeekRange] = Nil
	
	@Type(`type` = "uk.ac.warwick.tabula.data.model.groups.DayOfWeekUserType")
	@NotNull
	var day: DayOfWeek = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	@NotNull
	var startTime: LocalTime = _
	
	@Type(`type` = "org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	@NotNull
	var endTime: LocalTime = _
	
	// TODO We're storing locations as Strings atm, but do we need a more complex type to generate map links?
	var location: String = _
	
	var title: String = _
	
	def isSingleEvent = weekRanges.size == 1 && weekRanges.head.isSingleWeek
		
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "tutorsgroup_id")
	var tutors: UserGroup = new UserGroup
	
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
    tutors.members == other.tutors.members
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
    newEvent.tutors = tutors.duplicate()
    newEvent.weekRanges = weekRanges

    newEvent
  }

}