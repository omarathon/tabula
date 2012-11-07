package uk.ac.warwick.courses.helpers
import org.apache.log4j.Logger
import uk.ac.warwick.courses.helpers.Stopwatches._
import org.apache.log4j.Priority

trait Logging {
	@transient val loggerName = this.getClass.getName
	@transient lazy val logger = Logger.getLogger(loggerName)
	@transient lazy val debugEnabled = logger.isDebugEnabled

	@transient val Info = Priority.INFO
	@transient val Debug = Priority.DEBUG

	/**
	 * Logs a debug message, with the given arguments inserted into the
	 * format placeholders in the message. Checks debugEnabled for you,
	 * so no need to do that.
	 */
	def debug(message: String, arguments: Any*) =
		if (debugEnabled) logger.debug(message format (arguments: _*))

	/**
	 * Log an info message with the size of a collection.
	 * Returns the collection so you can wrap it with this without having
	 * to break out into a variable.
	 */
	def logSize[T](seq: Seq[T], level: Priority = Info)(implicit m: Manifest[T]) = {
		logger.log(level, "Collection of " + m.erasure.getClass.getSimpleName + "s: " + seq.size)
		seq
	}

	/**
	 * For logging the result of a function without having to break it
	 * out into multiple lines.
	 */
	def debugResult[T](description: String, result: T): T = {
		debug("%s: %s", description, result)
		result
	}

	/**
	 * Wrap some code in a stopwatch, logging some timing info
	 * if it takes longer than minMillis.
	 */
	def benchmark[T](description: String, level: Priority = Info, minMillis: Int = 0)(fn: => T): T = {
		if (!Logging.benchmarking) return fn
		val s = StopWatch()
		try s.record(description) {
			fn
		} finally {
			if (s.getTotalTimeMillis > minMillis) {
				logger.log(level, s.prettyPrint)
			}
		}
	}

}

object Logging {
	var benchmarking = true
}
