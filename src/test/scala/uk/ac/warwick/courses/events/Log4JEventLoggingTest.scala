package uk.ac.warwick.courses.events
import uk.ac.warwick.courses.TestBase
import org.apache.log4j.Logger
import org.apache.log4j.WriterAppender
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.StringWriter
import uk.ac.warwick.courses.commands.NullCommand
import org.apache.log4j.PatternLayout

class Log4JEventLoggingTest extends TestBase {

	val appender = new WriterAppender(new PatternLayout("%m"), new StringWriter())
	
	val listener = new Log4JEventListener
	
	@Before def attachAppender {
		listener.logger.addAppender(appender)
	}
	
	@After def detachAppender {
		listener.logger.removeAppender(appender)
	}
	
	@Test def writesLogs {
		val writer = new StringWriter
		appender.setWriter(writer)
		listener.afterCommand(new NullCommand, null)
		writer.toString should include ("event=NullCommand")
	}
	
}