package uk.ac.warwick.courses.events

import java.util.{List => JList}
import collection.JavaConversions._

import uk.ac.warwick.courses.commands.Describable

class CompositeEventListener(val listeners:JList[EventListener]) extends EventListener {

	override def beforeCommand(command: Describable) = 
		for (listener <- listeners) listener.beforeCommand(command)

	override def afterCommand(command: Describable, returnValue: Any) = 
		for (listener <- listeners) listener.afterCommand(command, returnValue)

	override def onException(command: Describable, exception: Throwable) = 
		for (listener <- listeners) listener.onException(command, exception)

}