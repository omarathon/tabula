package uk.ac.warwick.tabula.system.exceptions

import org.apache.log4j.WriterAppender
import java.io.StringWriter
import org.apache.log4j.Appender
import org.junit.After
import org.junit.Before
import org.apache.log4j.SimpleLayout
import org.apache.log4j.Logger
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException

class LoggingExceptionHandlerTest extends TestBase {
	
	val handler = new LoggingExceptionHandler

	var writer: StringWriter = _
	var appender: Appender = _
	
	@Before def setup {
		writer = new StringWriter
		
		appender = new WriterAppender(new SimpleLayout, writer)
		handler.logger.addAppender(appender)
	}
	
	@After def tearDown {
		if (appender != null) handler.logger.removeAppender(appender)
	}
	
	@Test def userError {
		val context = ExceptionContext("1", new ItemNotFoundException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))
		
		handler.exception(context)
		
		writer.getBuffer.toString.trim.split("\n").head should be ("DEBUG - User error")
	}
	
	@Test def handledException {
		val context = ExceptionContext("1", new MaintenanceModeEnabledException(None, None), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))
		
		handler.exception(context)
		
		writer.getBuffer.toString.trim.split("\n").head should be ("DEBUG - Handled exception")
	}
	
	@Test def normalException {
		val context = ExceptionContext("1", new RuntimeException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))
		
		handler.exception(context)
		
		writer.getBuffer.toString.trim.split("\n").head should be ("ERROR - Exception 1")
	}
	
}