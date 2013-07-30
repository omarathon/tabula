package uk.ac.warwick.tabula.system.exceptions

import org.apache.log4j._
import java.io.StringWriter
import org.junit.After
import org.junit.Before
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException

class LoggingExceptionHandlerTest extends TestBase {

	val handler = new LoggingExceptionHandler
	val lineSeparator = System.getProperty("line.separator")

	var writer: StringWriter = _
	var appender: Appender = _

	@Before def setup {
		writer = new StringWriter

		appender = new WriterAppender(new SimpleLayout, writer)
		handler.logger.addAppender(appender)
		handler.logger.setLevel(Level.DEBUG)
	}

	@After def tearDown {
		if (appender != null) handler.logger.removeAppender(appender)
		handler.logger.setLevel(null)
	}

	@Test def userError {
		val context = ExceptionContext("1", new ItemNotFoundException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		writer.getBuffer.toString.trim.split(lineSeparator).head should be ("DEBUG - User error")
	}

	@Test def handledException {
		val context = ExceptionContext("1", new MaintenanceModeEnabledException(None, None), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		writer.getBuffer.toString.trim.split(lineSeparator).head should be ("DEBUG - Handled exception")
	}

	@Test def normalException {
		val context = ExceptionContext("1", new RuntimeException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)

		writer.getBuffer.toString.trim.split(lineSeparator).head should be ("ERROR - Exception 1")
	}

}