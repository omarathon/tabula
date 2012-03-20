package uk.ac.warwick.courses.helpers

import uk.ac.warwick.util.core.StopWatch

object Stopwatches {
	class EnhancedStopwatch(val stopwatch:StopWatch) {
		def record(taskName:String)(work: =>Unit) {
			try {
				stopwatch.start(taskName)
				work
			} finally {
				stopwatch.stop();
			}
		}
	}
	
	implicit def ToEnhancedStopwatch(s:StopWatch) = new EnhancedStopwatch(s)
}