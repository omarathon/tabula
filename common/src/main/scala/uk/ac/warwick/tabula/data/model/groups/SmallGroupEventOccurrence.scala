package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._

import org.hibernate.annotations.BatchSize
import org.joda.time.{LocalDate, LocalDateTime}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

@Access(AccessType.FIELD)
@Entity
@Table(uniqueConstraints = Array(
	new UniqueConstraint(columnNames = Array("event_id", "week"))
))
class SmallGroupEventOccurrence extends GeneratedId with PermissionsTarget with Serializable with ToEntityReference {

	override type Entity = SmallGroupEventOccurrence

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="event_id", updatable = false)
	var event: SmallGroupEvent = _

	var week: SmallGroupEventOccurrence.WeekNumber = _

	@OneToMany(mappedBy = "occurrence", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var attendance: JSet[SmallGroupEventAttendance] = JHashSet()

	def permissionsParents = Stream(event)

	def date: Option[LocalDate] = event.dateForWeek(week)
	@deprecated("Use startDateTime or endDateTime", since = "206")
	def dateTime: Option[LocalDateTime] = startDateTime
	def startDateTime: Option[LocalDateTime] = event.startDateTimeForWeek(week)
	def endDateTime: Option[LocalDateTime] = event.endDateTimeForWeek(week)

	override def humanReadableId: String = s"${event.humanReadableId} week $week"

	override def toEntityReference: SmallGroupEventOcurrenceEntityReference = new SmallGroupEventOcurrenceEntityReference().put(this)
}

object SmallGroupEventOccurrence {
	type WeekNumber = Int
}
