package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import scala.collection.JavaConverters._
import uk.org.lidalia.slf4jtest.TestLoggerFactory
import uk.org.lidalia.slf4jtest.LoggingEvent

// scalastyle:off magic.number
class LoggingTest extends TestBase with Logging {

	val testLogger = TestLoggerFactory.getTestLogger(logger.getName)

	@Test def itWorks {
		debug("my message")
		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.debug("my message"))
		
		debug("my message %s (%s)", "my argument 1", "my argument 2")
		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.debug("my message my argument 1 (my argument 2)"))
		
		logSize(Seq("a", "b", "c"))
		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.info("Collection of Strings: 3"))
		
		debugResult("my magical thing", true)
		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.debug("my magical thing: true"))
		
		// Turn benchmarking off
		Logging.benchmarking = false
		var ranFn = false
		benchmark("my magical thing") {
			Thread.sleep(50)
			ranFn = true
		}
		
		// The last thing is still there because benchmarking is disabled, but we still ran the function
		ranFn should be (true)
		testLogger.getLoggingEvents.asScala.last should be (LoggingEvent.debug("my magical thing: true"))
	}

}