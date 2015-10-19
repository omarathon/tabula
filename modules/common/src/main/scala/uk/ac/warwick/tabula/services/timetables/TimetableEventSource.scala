package uk.ac.warwick.tabula.services.timetables

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.util.Try

trait TimetableEventSource[A <: Member] {
	def eventsFor(member: A, currentUser: CurrentUser, context: TimetableEvent.Context): Try[Seq[TimetableEvent]]
}