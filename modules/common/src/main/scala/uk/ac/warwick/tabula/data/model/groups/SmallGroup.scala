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
import javax.validation.constraints.NotNull

object SmallGroup {
	final val NotDeletedFilter = "notDeleted"
}

/**
 * Represents a single small teaching group within a group set.
 */
@FilterDef(name = SmallGroup.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = SmallGroup.NotDeletedFilter)
@Entity
@AccessType("field")
class SmallGroup extends GeneratedId with CanBeDeleted with ToString with PermissionsTarget with Serializable {
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
	var events: JList[SmallGroupEvent] = JArrayList()
	
	def permissionsParents = Option(groupSet).toStream
		
	@OneToOne(cascade = Array(ALL))
	@JoinColumn(name = "studentsgroup_id")
	var students: UserGroup = new UserGroup

	def toStringProps = Seq(
		"id" -> id,
		"name" -> name,
		"set" -> groupSet)
	
}