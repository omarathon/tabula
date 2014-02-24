package uk.ac.warwick.tabula

import org.junit.Test


/**
 * This used to be a test for some hand-rolled observer classes,
 * but now it is just testing the scala.react library.
 */
class ObserverTest extends TestBase {

	import Reactor._
	
	class Service {
		var changedState = EventSource[Boolean]
		def updateState(state:Boolean) {
			changedState.emit(state)
		}
	}
	
	class ServiceObserver(service:Service) {
		var enabled = false

		service.changedState.observe { value =>
			enabled = value
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