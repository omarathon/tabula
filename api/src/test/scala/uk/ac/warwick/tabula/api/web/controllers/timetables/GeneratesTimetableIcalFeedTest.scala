package uk.ac.warwick.tabula.api.web.controllers.timetables

import net.fortuna.ical4j.data.CalendarOutputter
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.services.timetables.{EventOccurrenceService, EventOccurrenceServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class GeneratesTimetableIcalFeedTest extends TestBase with Mockito {

	val generator = new GeneratesTimetableIcalFeed with EventOccurrenceServiceComponent {
		val eventOccurrenceService: EventOccurrenceService = mock[EventOccurrenceService]
	}

	@Test
	def emptyTimetableValidates() {
		val ical = generator.getIcalFeed(Seq(), Fixtures.staff("1234"))

		new CalendarOutputter().output(ical, new MockHttpServletResponse().getWriter)
		// doesn't throw an exception
	}

}
