package uk.ac.warwick.tabula.events

import org.junit.After
import uk.ac.warwick.tabula.commands.DescriptionImpl
import uk.ac.warwick.tabula.commands.NullCommand
import uk.ac.warwick.tabula.{TestLoggerFactory, TestBase}

import org.joda.time.DateTime

class SLF4JEventLoggingTest extends TestBase {

	val testLogger = TestLoggerFactory.getTestLogger("uk.ac.warwick.tabula.AUDIT")
	val listener = new SLF4JEventListener

	@Test def writesLogs {
		val command = new NullCommand().describedAs {(d) =>
			d.properties("mykey" -> "jibberjabber")
		}

		val description = new DescriptionImpl
		command.describe(description)
		
		val event = new Event("1235", command.eventName, null, null, description.allProperties, new DateTime)
		
		listener.afterCommand(event, null)
		TestLoggerFactory.retrieveEvents(testLogger).map(_.getMessage) should be (Seq("event=Null mykey=jibberjabber"))
	}
	
}