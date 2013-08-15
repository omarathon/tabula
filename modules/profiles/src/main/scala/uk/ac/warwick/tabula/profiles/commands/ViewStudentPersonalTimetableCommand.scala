package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, Appliable, CommandInternal}
import uk.ac.warwick.tabula.profiles.services.timetables._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.{AutowiringTermFactoryComponent, TermAwareWeekToDateConverterComponent, AutowiringUserLookupComponent, AutowiringSmallGroupServiceComponent}
import org.joda.time.{Interval, LocalDate}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

class ViewStudentPersonalTimetableCommandImpl extends CommandInternal[Seq[EventOccurrence]]{
	this:StudentTimetableEventSourceComponent with EventOccurrenceComponent=>

	var student:StudentMember = _
	var start:LocalDate = LocalDate.now
	var end:LocalDate = start.plusMonths(1)

	def eventsToOccurrences:TimetableEvent=>Seq[EventOccurrence] = eventOccurenceService.fromTimetableEvent(_,new Interval(start.toDateTimeAtStartOfDay, end.toDateTimeAtStartOfDay))

	protected def applyInternal(): Seq[EventOccurrence] = {
		val timetableEvents = studentTimetableEventSource.eventsFor(student)
		val occurences = timetableEvents flatMap eventsToOccurrences
		occurences.sortBy(_.start)
	}
}

object ViewStudentPersonalTimetableCommand{

	// mmm, cake.
	def apply:Appliable[Seq[EventOccurrence]] = {

		new ViewStudentPersonalTimetableCommandImpl
			with ComposableCommand[Seq[EventOccurrence]]
		  with PubliclyVisiblePermissions
		  with Unaudited
		  with CombinedStudentTimetableEventSourceComponent
		  with SmallGroupEventTimetableEventSourceComponent
		  with ScientiaHttpTimetableFetchingServiceComponent
		  with EventOccurrenceComponent
		  with TermAwareWeekToDateConverterComponent
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
		  with AutowiringTermFactoryComponent
	}
}


