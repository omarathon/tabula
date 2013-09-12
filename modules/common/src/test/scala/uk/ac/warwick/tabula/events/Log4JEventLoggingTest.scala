package uk.ac.warwick.tabula.events

import java.io.StringWriter
import org.apache.log4j.{Level, Logger, PatternLayout, WriterAppender}
import org.junit.After
import org.junit.Before
import org.junit.Test
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.commands.NullCommand
import uk.ac.warwick.tabula.TestBase



import org.joda.time.DateTime

class Log4JEventLoggingTest extends TestBase {

	val appender = new WriterAppender(new PatternLayout("%m"), new StringWriter())
	
	val listener = new Log4JEventListener
	
	@Before def attachAppender {
		listener.logger.addAppender(appender)
		listener.logger.setLevel(Level.DEBUG)
	}
	
	@After def detachAppender {
		listener.logger.removeAppender(appender)
		listener.logger.setLevel(null)
	}
	
	@Test def writesLogs {
		val writer = new StringWriter
		val command = new NullCommand().describedAs {(d) =>
			d.properties("mykey" -> "jibberjabber")
		}
		appender.setWriter(writer)
		
		val description = new DescriptionImpl
		command.describe(description)
		
		val event = new Event("1235", command.eventName, null, null, description.allProperties.toMap, new DateTime) 
		
		listener.afterCommand(event, null)
		writer.toString should include ("event=Null mykey=jibberjabber")
	}
	
}