package uk.ac.warwick.tabula


/**
 * Replacement for React.
 */
object Reactor {
	def EventSource[A]() = new EventSource[A]

	class EventSource[A]() {
		type Observer = (A) => Unit
		var observers = List[Observer]()
		def observe(ob: Observer) {
			observers = (ob :: observers)
		}
		def emit(a:A) {
			observers.foreach { _(a) }
		}
	}

}


