package uk.ac.warwick.tabula.events

trait EventListener {
	def beforeCommand(event: Event)
	def afterCommand(event: Event, returnValue: Any, beforeEvent: Event)
	def onException(event: Event, exception: Throwable)
}