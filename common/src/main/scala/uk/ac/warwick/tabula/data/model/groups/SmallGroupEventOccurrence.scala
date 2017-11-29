package uk.ac.warwick.tabula.data.model.groups

import javax.persistence._

import org.hibernate.annotations.BatchSize
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicWeek
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

	def date: Option[LocalDate] = {
		date(event.group.groupSet.academicYear.weeks)
	}
	def date(weeksForYear: Map[Int, AcademicWeek]): Option[LocalDate] = {
		event.day match {
			case null => None
			case d: DayOfWeek =>
				def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek) =
					weeksForYear(weekNumber).firstDay.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

				Option(weekNumberToDate(week, event.day))
		}
	}

	def dateTime: Option[DateTime] = date.map(_.toDateTime(event.startTime))
	def dateTime(weeksForYear: Map[Int, AcademicWeek]): Option[DateTime] = date(weeksForYear).map(_.toDateTime(event.startTime))

	override def humanReadableId: String = s"${event.humanReadableId} week $week"

	override def toEntityReference: SmallGroupEventOcurrenceEntityReference = new SmallGroupEventOcurrenceEntityReference().put(this)
}

object SmallGroupEventOccurrence {
	type WeekNumber = Int
}
