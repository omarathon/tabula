package uk.ac.warwick.courses.events
import uk.ac.warwick.courses.commands.Describable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import scala.reflect.BeanProperty

/**
 * Gives a class the ability to record events from a Describable object.
 */
trait EventHandling {
	var listener:EventListener
	
	def recordEvent[T](d:Describable)(f: =>T): T = {
		val event = Event.fromDescribable(d)
		try {
			listener.beforeCommand(event)
			val result = f
			listener.afterCommand(event, result)
			return result
		} catch {
			case e:Throwable => {
				listener.onException(event, e)
				throw e
			}
		}
	}
}