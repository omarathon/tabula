package uk.ac.warwick.courses.events
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.commands.Description

trait EventListener {
	def beforeCommand(event: Event)
	def afterCommand(event: Event, returnValue: Any)
	def onException(event: Event, exception: Throwable)
}