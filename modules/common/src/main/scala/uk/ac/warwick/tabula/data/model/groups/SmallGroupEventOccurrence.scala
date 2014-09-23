package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._

import org.hibernate.annotations.{AccessType, BatchSize}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.TermService

@AccessType("field")
@Entity
@Table(uniqueConstraints = Array(
	new UniqueConstraint(columnNames = Array("event_id", "week"))
))
class SmallGroupEventOccurrence extends GeneratedId with PermissionsTarget with Serializable with ToEntityReference {

	override type Entity = SmallGroupEventOccurrence

	@transient var termService = Wire[TermService]

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="event_id")
	var event: SmallGroupEvent = _

	var week: SmallGroupEventOccurrence.WeekNumber = _
	
	@OneToMany(mappedBy = "occurrence", cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=200)
	var attendance: JSet[SmallGroupEventAttendance] = JHashSet()

	def permissionsParents = Stream(event)

	def date: Option[LocalDate] = {
		event.day match {
			case null => None
			case d: DayOfWeek =>
				val weeksForYear = termService.getAcademicWeeksForYear(event.group.groupSet.academicYear.dateInTermOne).toMap
				def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
					weeksForYear(weekNumber).getStart.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

				Option(weekNumberToDate(week, event.day).toLocalDate)
		}
	}

	def dateTime: Option[DateTime] = date.map(_.toDateTime(event.startTime))

	override def toEntityReference = new SmallGroupEventOcurrenceEntityReference().put(this)
}

object SmallGroupEventOccurrence {
	type WeekNumber = Int
}
