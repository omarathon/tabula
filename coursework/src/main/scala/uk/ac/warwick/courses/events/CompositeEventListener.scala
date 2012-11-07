package uk.ac.warwick.courses.events

import uk.ac.warwick.courses.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.courses.commands.Describable
import uk.ac.warwick.courses.commands.Description

class CompositeEventListener(val listeners: JList[EventListener]) extends EventListener {

	override def beforeCommand(event: Event) =
		for (listener <- listeners) listener.beforeCommand(event)

	override def afterCommand(event: Event, returnValue: Any) =
		for (listener <- listeners) listener.afterCommand(event, returnValue)

	override def onException(event: Event, exception: Throwable) =
		for (listener <- listeners) listener.onException(event, exception)

}