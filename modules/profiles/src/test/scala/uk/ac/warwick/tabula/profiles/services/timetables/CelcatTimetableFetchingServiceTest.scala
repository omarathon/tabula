package uk.ac.warwick.tabula.profiles.services.timetables

import dispatch.classic.Credentials
import org.apache.http.auth.AuthScope
import org.joda.time.LocalTime
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.services.permissions.CacheStrategyComponent
import uk.ac.warwick.tabula.services.{TermServiceImpl, TermServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.timetables.{TimetableEventType, TimetableEvent}
import uk.ac.warwick.util.cache.Caches.CacheStrategy
import uk.ac.warwick.tabula.helpers.StringUtils._

class CelcatTimetableFetchingServiceTest extends TestBase {

	val service = new CelcatHttpTimetableFetchingService(new CelcatConfiguration {
		val departmentConfiguration =	Map(
			"ch" -> CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/chem-timetables"),
			"es" -> CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/eng-timetables")
		)
		lazy val authScope = new AuthScope("www2.warwick.ac.uk", 443)
		lazy val credentials = Credentials("username", "password")
		val cacheEnabled = false
	}) with UserLookupComponent with TermServiceComponent with CacheStrategyComponent {
		val userLookup = new MockUserLookup
		val termService = new TermServiceImpl
		val cacheStrategy = CacheStrategy.InMemoryOnly
	}

	@Test def parseICal {
		val events = service.parseICal(resourceAsStream("1313406.ics"), CelcatDepartmentConfiguration("https://www2.warwick.ac.uk/appdata/chem-timetables"))
		events.size should be (142)

		val combined = service.combineIdenticalEvents(events).sortBy { event => (event.weekRanges.head.minWeek, event.day.jodaDayOfWeek, event.startTime.getMillisOfDay)}
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
		combined(0) should be (TimetableEvent(
			"ES186 - AMP/DAH/DJB/MVC/NGS",
			"",
			"Engineering Skills, Induction",
			TimetableEventType.Other("Briefing"),
			Seq(WeekRange(1)),
			DayOfWeek.Monday,
			new LocalTime(11, 15),
			new LocalTime(13, 0),
			Some("R021"),
			Some("ES186"),
			Nil,
			AcademicYear.parse("13/14")
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
			"ES186 - SJL",
			"",
			"Engineering Skills, Support sessions for students without A-level Physics",
			TimetableEventType.Lecture,
			Seq(WeekRange(2, 3)),
			DayOfWeek.Monday,
			new LocalTime(10, 0),
			new LocalTime(11, 0),
			Some("P521"),
			Some("ES186"),
			Nil,
			AcademicYear.parse("13/14")
		))
	}

}