package uk.ac.warwick.courses.helpers

import uk.ac.warwick.util

object Stopwatches {
	
	class EnhancedStopwatch(val stopwatch:util.core.StopWatch) {
		def record[T](taskName:String)(work: =>T) : T = {
			try {
				stopwatch.start(taskName)
				work
			} finally {
				stopwatch.stop();
			}
		}
	}
	
	implicit def ToEnhancedStopwatch(s:util.core.StopWatch) = new EnhancedStopwatch(s)
	
	object StopWatch {
		def apply() = new util.core.StopWatch()
	}
}