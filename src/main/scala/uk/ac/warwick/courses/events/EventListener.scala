package uk.ac.warwick.courses.events
import uk.ac.warwick.courses.commands.Describable

trait EventListener {
	def beforeCommand(command:Describable)
	def afterCommand(command:Describable, returnValue:Any)
	def onException(command:Describable, exception:Throwable)
}