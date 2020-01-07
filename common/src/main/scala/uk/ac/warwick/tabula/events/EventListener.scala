package uk.ac.warwick.tabula.events

trait EventListener {
  def beforeCommand(event: Event): Unit

  def afterCommand(event: Event, returnValue: Any, beforeEvent: Event): Unit

  def onException(event: Event, exception: Throwable): Unit
}