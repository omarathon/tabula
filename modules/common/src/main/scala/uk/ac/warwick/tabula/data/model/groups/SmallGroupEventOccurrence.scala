package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._
import org.hibernate.annotations.{AccessType, BatchSize}
import uk.ac.warwick.tabula.data.model.{UserGroup, GeneratedId}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports._
import javax.persistence.CascadeType._
import java.lang.annotation.Annotation

@AccessType("field")
@Entity
@Table(uniqueConstraints = Array(
	new UniqueConstraint(columnNames = Array("event_id", "week"))
))
class SmallGroupEventOccurrence extends GeneratedId with PermissionsTarget with Serializable {
	@ManyToOne
	@JoinColumn(name="event_id")
	var event: SmallGroupEvent = _

	var week: SmallGroupEventOccurrence.WeekNumber = _
	
	@OneToMany(mappedBy = "occurrence", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var attendance: JSet[SmallGroupEventAttendance] = JHashSet()

	def permissionsParents = Stream(event)

}

object SmallGroupEventOccurrence {
	type WeekNumber = Int
}
