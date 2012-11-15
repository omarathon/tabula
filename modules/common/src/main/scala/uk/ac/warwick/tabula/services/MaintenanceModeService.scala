package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import scala.react.EventSource
import uk.ac.warwick.tabula.system.exceptions.HandledException

trait MaintenanceModeService {

	/** Enable maintenance mode. **/
	def enable

	/** Disable maintenance mode. **/
	def disable

	/** Returns whether maintenance mode is enabled. */
	@BeanProperty def enabled: Boolean

	/**
	 * An EventSource to which you can attach a listener to find
	 * out when maintenance mode goes on and off.
	 */
	val changingState: EventSource[Boolean]

	/**
	 * Returns an Exception object suitable for throwing when trying
	 * to do an unsupported op while in maintenance mode. Only returns
	 * it; you need to throw it yourself. Like a dog!
	 */
	def exception(): Exception

	@BeanProperty var until: Option[DateTime]
	@BeanProperty var message: Option[String]
}

@Service
class MaintenanceModeServiceImpl extends MaintenanceModeService with MaintenanceStatus {
	private var _enabled: Boolean = false;

	@BeanProperty def enabled: Boolean = _enabled
	@BeanProperty var until: Option[DateTime] = None
	@BeanProperty var message: Option[String] = None

	// for other classes to listen to changes to maintenance mode.
	val changingState = EventSource[Boolean]

	def exception() = new MaintenanceModeEnabledException(
		until,
		message)

	private def notEnabled = new IllegalStateException("Maintenance not enabled")

	def enable {
		if (!_enabled) {
			_enabled = true
			changingState.emit(_enabled)
		}
	}

	def disable {
		if (_enabled) {
			_enabled = false
			changingState.emit(_enabled)
		}
	}
}

trait MaintenanceStatus {
	def enabled: Boolean
	def until: Option[DateTime]
	def message: Option[String]
}

/**
 * Exception thrown when a command tries to run during
 * maintenance mode, and it's not readonly. The view handler
 * should handle this exception specially, showing a nice message.
 *
 * Holds onto some info about the maintenance, since error views are
 * only provided with the thrown exception.
 */
class MaintenanceModeEnabledException(val until: Option[DateTime], val message: Option[String])
	extends RuntimeException
	with HandledException {

	def getMessageOrEmpty = message.getOrElse("")

}