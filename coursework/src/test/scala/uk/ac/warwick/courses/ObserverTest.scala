package uk.ac.warwick.courses

import org.junit.Test
import scala.react.Observing
import scala.react.EventSource

/**
 * This used to be a test for some hand-rolled observer classes,
 * but now it is just testing the scala.react library.
 */
class ObserverTest extends TestBase {
	
	class Service {
		var changedState = EventSource[Boolean]
		def updateState(state:Boolean) {
			changedState.emit(state)
		}
	}
	
	class ServiceObserver(service:Service) extends Observing {
		var enabled = false
		
		val observerHandle = observe(service.changedState) { value =>
			enabled = value
			true
		}
	}
	
	@Test def listening {
		val service = new Service
		val observer = new ServiceObserver(service)
		
		observer.enabled should be (false)
		service updateState true
		observer.enabled should be (true)
		service updateState false
		observer.enabled should be (false)
		
	}

}