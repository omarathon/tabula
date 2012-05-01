package uk.ac.warwick.courses.services

import org.springframework.stereotype.Service
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import scala.react.EventSource
import uk.ac.warwick.courses.system.exceptions.HandledException

@Service
class MaintenanceModeService extends MaintenanceStatus {
	private var _enabled:Boolean = false;
	
	@BeanProperty def enabled:Boolean = _enabled
	@BeanProperty var until:Option[DateTime] = None
	@BeanProperty var message:Option[String] = None
	
	// for other classes to listen to changes to maintenance mode.
	val changingState = EventSource[Boolean]
	
	def exception() = new MaintenanceModeEnabledException(
			until, 
			message
		)
	
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
	def enabled:Boolean
	def until:Option[DateTime]
	def message:Option[String]
}

/**
 * Exception thrown when a command tries to run during
 * maintenance mode, and it's not readonly. The view handler
 * should handle this exception specially, showing a nice message.
 * 
 * Holds onto some info about the maintenance, since error views are
 * only provided with the thrown exception.
 */
class MaintenanceModeEnabledException(val until:Option[DateTime], val message:Option[String]) 
	extends RuntimeException 
	with HandledException {
	
	def getMessageOrEmpty = message.getOrElse("")
	
}