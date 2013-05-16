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
import uk.ac.warwick.tabula.roles.SmallGroupMemberRoleDefinition
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.tabula.data.model.groups.WeekRange._
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.roles.SmallGroupTutorRoleDefinition
import javax.validation.constraints.NotNull

@Entity
@AccessType("field")
class SmallGroupEvent extends GeneratedId with ToString with PermissionsTarget {
	
	@transient var permissionsService = Wire[PermissionsService]

	def this(_group: SmallGroup) {
		this()
		this.group = _group
	}
	
	@ManyToOne
	@JoinColumn(name = "group_id")
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
	
	@transient
	lazy val tutors = permissionsService.ensureUserGroupFor(this, SmallGroupTutorRoleDefinition)
	
	def permissionsParents = Option(group).toStream
	
	def toStringProps = Seq(
		"id" -> id,
		"weekRanges" -> weekRanges,
		"day" -> day,
		"startTime" -> startTime,
		"endTime" -> endTime)
	
}