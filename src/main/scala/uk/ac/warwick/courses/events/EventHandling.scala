package uk.ac.warwick.courses.events
import uk.ac.warwick.courses.commands.Describable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.commands.Unaudited

/**
 * Gives a class the ability to record events from a Describable object.
 */
trait EventHandling {
	@Autowired @BeanProperty var listener:EventListener = _

	/**
	 * Records the various stages of an event: before, and either
	 * after or error. All of them should have the same eventid to
	 * join them together (though they also have unique primary keys).
	 */
	def recordEvent[T](d:Describable)(f: =>T): T = 
		d match {
			case _:Unaudited => f // don't audit unaudited events!
			case _ => {
				val event = Event.fromDescribable(d)
				try {
					listener.beforeCommand(event)
					val result = f
					val resultEvent = Event.resultFromDescribable(d, event.id)
					listener.afterCommand(resultEvent, result)
					return result
				} catch {
					case e:Throwable => {
						listener.onException(event, e)
						throw e
					}
				}
			}
		}

}