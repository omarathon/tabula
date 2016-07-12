package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.{Http, Credentials}
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.util.CompatibilityHints
import org.apache.commons.io.IOUtils
import org.apache.http.entity.InputStreamEntity
import org.apache.http.message.BasicHttpResponse
import org.apache.http.{HttpVersion, HttpRequest, HttpHost}
import org.apache.http.auth.AuthScope
import org.apache.http.client.HttpClient
import org.joda.time.LocalTime
import org.mockito.Matchers
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.NamedLocation
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.CacheStrategyComponent
import uk.ac.warwick.tabula.timetables.{RelatedUrl, TimetableEvent, TimetableEventType}
import uk.ac.warwick.util.cache.Caches.CacheStrategy

class CelcatTimetableFetchingServiceTest extends TestBase with Mockito {

	val module = Fixtures.module("es186")

	val httpClient = mock[HttpClient]

	val service = new CelcatHttpTimetableFetchingService(new CelcatConfiguration {
		val departmentConfiguration =	Map(
			"ch" -> CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/chem-timetables")
		)
		lazy val authScope = new AuthScope("www2.warwick.ac.uk", 443)
		lazy val credentials = Credentials("username", "password")
		val cacheEnabled = false
	}) with UserLookupComponent with TermServiceComponent with CacheStrategyComponent with LocationFetchingServiceComponent with ModuleAndDepartmentServiceComponent with DispatchHttpClientComponent {
		val userLookup = new MockUserLookup
		val termService = new TermServiceImpl
		val cacheStrategy = CacheStrategy.InMemoryOnly
		val locationFetchingService = new LocationFetchingService {
			def locationFor(name: String) = NamedLocation(name)
		}
		val moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		moduleAndDepartmentService.getModulesByCodes(Matchers.any[Seq[String]]) answers {codes =>
			codes.asInstanceOf[Seq[String]].map(code => Fixtures.module(code))
		}

		override val httpClient: Http = new Http {
			override def make_client = CelcatTimetableFetchingServiceTest.this.httpClient
		}
	}

	@Test def parseICal() {
		val events = service.parseICal(resourceAsStream("1313406.ics"), CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/chem-timetables"))(service.termService)
		events.events.size should be (142)

		val combined = service.combineIdenticalEvents(events).events.sorted
		combined.size should be (136)

		// Check that the first few events are as expected

		/*
		BEGIN:VEVENT
		DTSTAMP:20140811T221200Z
		SEQUENCE:0
		TRANSP:OPAQUE
		LAST-MODIFIED:20140811T221200Z
		DTSTART;TZID=Europe/London:20130930T111500
		DTEND;TZID=Europe/London:20130930T130000
		SUMMARY:ES186 - AMP/DAH/DJB/MVC/NGS
		UID:CT-1313406-6447-2013-09-30-R021@eng.warwick.ac.uk
		DESCRIPTION:Engineering Skills, Induction
		CATEGORIES:Briefing
		LOCATION:R021
		END:VEVENT
		 */
		combined.head should be (TimetableEvent(
			"CT-1313406-6447-2013-09-30-R021@eng.warwick.ac.uk",
			"ES186 - AMP/DAH/DJB/MVC/NGS",
			"",
			"Engineering Skills, Induction",
			TimetableEventType.Other("Briefing"),
			Seq(WeekRange(1)),
			DayOfWeek.Monday,
			new LocalTime(11, 15),
			new LocalTime(13, 0),
			Some(NamedLocation("R021")),
			TimetableEvent.Parent(Some(module)),
			None,
			Nil,
			Nil,
			AcademicYear.parse("13/14"),
			relatedUrl = RelatedUrl("", None)
		))

		/*
		BEGIN:VEVENT
		DTSTAMP:20140811T221200Z
		SEQUENCE:0
		TRANSP:OPAQUE
		LAST-MODIFIED:20140811T221200Z
		DTSTART;TZID=Europe/London:20131007T100000
		DTEND;TZID=Europe/London:20131007T110000
		SUMMARY:ES186 - SJL
		UID:CT-1313406-6147-2013-10-07-P521@eng.warwick.ac.uk
		DESCRIPTION:Engineering Skills, Support sessions for students without A-level Physics
		CATEGORIES:Lecture
		LOCATION:P521
		RRULE:FREQ=WEEKLY;COUNT=2;BYDAY=MO
		END:VEVENT
		 */
		combined(15) should be (TimetableEvent(
			"CT-1313406-6147-2013-10-07-P521@eng.warwick.ac.uk",
			"ES186 - SJL",
			"",
			"Engineering Skills, Support sessions for students without A-level Physics",
			TimetableEventType.Lecture,
			Seq(WeekRange(2, 3), WeekRange(5, 10)),
			DayOfWeek.Monday,
			new LocalTime(10, 0),
			new LocalTime(11, 0),
			Some(NamedLocation("P521")),
			TimetableEvent.Parent(Some(module)),
			None,
			Nil,
			Nil,
			AcademicYear.parse("13/14"),
			relatedUrl = RelatedUrl("", None)
		))
	}

	@Test def tab2662() {
		val events = service.parseICal(resourceAsStream("duplicates.ics"), CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/chem-timetables"))(service.termService)
		events.events.size should be (2)

		val combined = service.combineIdenticalEvents(events).events.sorted
		combined.size should be (1)
		combined.head.weekRanges should be (Seq(WeekRange(1), WeekRange(3, 5)))
	}

	@Test def unusualEvent() {
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true)

		val builder = new CalendarBuilder
		val cal = builder.build(IOUtils.toInputStream(
			"""
				|BEGIN:VCALENDAR
				|BEGIN:VEVENT
				|DTSTAMP:20140914T220100Z
				|SEQUENCE:0
				|TRANSP:OPAQUE
				|LAST-MODIFIED:20140914T220100Z
				|DTSTART;TZID=Europe/London:20141001T090500
				|DTEND;TZID=Europe/London:20141001T185500
				|SUMMARY:Unavailable - Clark, Andrew
				|UID:CT-Clark-Andrew-20244-2014-10-01-@chem.warwick.ac.uk
				|DESCRIPTION:
				|CATEGORIES:
				|RRULE:FREQ=WEEKLY;COUNT=52;BYDAY=WE
				|END:VEVENT
				|END:VCALENDAR
			""".stripMargin.trim()))

		val parsed =
			CelcatHttpTimetableFetchingService.parseVEvent(
				cal.getComponent(Component.VEVENT).asInstanceOf[VEvent],
				Map(),
				CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/chem-timetables"),
				service.locationFetchingService,
				Map(module.code -> module),
				service.userLookup
			)(service.termService)

		parsed should be ('defined)
		parsed.get should be (TimetableEvent(
			"CT-Clark-Andrew-20244-2014-10-01-@chem.warwick.ac.uk",
			"Unavailable - Clark, Andrew",
			"",
			"Unavailable - Clark, Andrew",
			TimetableEventType.Other("Unavailable"),
			Seq(WeekRange(1, 52)),
			DayOfWeek.Wednesday,
			new LocalTime(9, 5),
			new LocalTime(18, 55),
			None,
			TimetableEvent.Parent(None),
			None,
			Nil,
			Nil,
			AcademicYear.parse("14/15"),
			relatedUrl = RelatedUrl("", None)
		))
	}

	@Test(timeout = 5000) def tab3899() {
		// Mock the BSV request
		val bsvResponse = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK")
		bsvResponse.setEntity(new InputStreamEntity(resourceAsStream("staff.bsv")))

		httpClient.execute(any[HttpHost], any[HttpRequest]) returns bsvResponse

		val events = service.parseICal(
			resourceAsStream("1524943.ics"),
			CelcatDepartmentConfiguration(
				baseUri = "https://www2.warwick.ac.uk/appdata/chem-timetables",
				staffListInBSV = true
			)
		)(service.termService)
		events.events.size should be (122)
	}

}