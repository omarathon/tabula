package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.TestBase
import org.apache.log4j.Logger
import org.apache.log4j.Appender
import org.apache.log4j.WriterAppender
import java.io.StringWriter
import org.apache.log4j.SimpleLayout
import org.junit.After
import org.junit.Before

// scalastyle:off magic.number
class LoggingTest extends TestBase with Logging {
	
	var writer: StringWriter = _
	var appender: Appender = _
	
	@Before def setup {
		writer = new StringWriter
		
		appender = new WriterAppender(new SimpleLayout, writer)
		logger.addAppender(appender)
	}
	
	@After def tearDown {
		if (appender != null) logger.removeAppender(appender)
	}
	
	@Test def itWorks {
		debug("my message")
		writer.getBuffer.toString.trim.split("\n").last should be ("DEBUG - my message")
		
		debug("my message %s (%s)", "my argument 1", "my argument 2")
		writer.getBuffer.toString.trim.split("\n").last should be ("DEBUG - my message my argument 1 (my argument 2)")
		
		logSize(Seq("a", "b", "c"))
		writer.getBuffer.toString.trim.split("\n").last should be ("INFO - Collection of Strings: 3")
		
		debugResult("my magical thing", true)
		writer.getBuffer.toString.trim.split("\n").last should be ("DEBUG - my magical thing: true")
		
		// Turn benchmarking off
		Logging.benchmarking = false
		var ranFn = false
		benchmark("my magical thing") {
			Thread.sleep(50)
			ranFn = true
		}
		
		// The last thing is still there because benchmarking is disabled, but we still ran the function
		ranFn should be (true)
		writer.getBuffer.toString.trim.split("\n").last should be ("DEBUG - my magical thing: true")
	}

}