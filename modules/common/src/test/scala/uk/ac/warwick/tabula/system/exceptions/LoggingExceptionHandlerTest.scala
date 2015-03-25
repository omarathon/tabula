package uk.ac.warwick.tabula.system.exceptions

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import uk.org.lidalia.slf4jtest.TestLoggerFactory
import uk.org.lidalia.slf4jext.Level

class LoggingExceptionHandlerTest extends TestBase {

	val testLogger = TestLoggerFactory.getTestLogger(classOf[LoggingExceptionHandler].getName)
	val handler = new LoggingExceptionHandler
	val lineSeparator = System.getProperty("line.separator")

	@Test def userError {
		val context = ExceptionContext("1", new ItemNotFoundException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		val logEvent = testLogger.getLoggingEvents.asScala.last
		logEvent.getMessage.split(lineSeparator).head should be ("User error")
		logEvent.getLevel should be (Level.DEBUG)
	}

	@Test def handledException {
		val context = ExceptionContext("1", new MaintenanceModeEnabledException(None, None), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		val logEvent = testLogger.getLoggingEvents.asScala.last
		logEvent.getMessage.split(lineSeparator).head should be ("Handled exception")
		logEvent.getLevel should be (Level.DEBUG)
	}

	@Test def normalException {
		val context = ExceptionContext("1", new RuntimeException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		val logEvent = testLogger.getLoggingEvents.asScala.last
		logEvent.getMessage.split(lineSeparator).head should be ("Exception 1")
		logEvent.getLevel should be (Level.ERROR)
	}

}