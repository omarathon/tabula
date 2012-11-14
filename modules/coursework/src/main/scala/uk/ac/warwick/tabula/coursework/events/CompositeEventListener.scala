package uk.ac.warwick.tabula.coursework.events

import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.commands.Describable
import uk.ac.warwick.tabula.coursework.commands.Description

class CompositeEventListener(val listeners: JList[EventListener]) extends EventListener {

	override def beforeCommand(event: Event) =
		for (listener <- listeners) listener.beforeCommand(event)

	override def afterCommand(event: Event, returnValue: Any) =
		for (listener <- listeners) listener.afterCommand(event, returnValue)

	override def onException(event: Event, exception: Throwable) =
		for (listener <- listeners) listener.onException(event, exception)

}