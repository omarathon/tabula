package uk.ac.warwick.tabula.helpers
import uk.ac.warwick.tabula.TestBase


import org.junit.Test
import org.joda.time.Duration
import org.joda.time.Duration._
import org.joda.time.Period
import org.joda.time.DateTime
import org.joda.time.PeriodType
import org.joda.time.Interval

import uk.ac.warwick.tabula.coursework.helpers.DurationFormatter

class DurationFormatterTest extends TestBase {
	@Test def format {		
		implicit val start = dateTime(2012, 5)
		
		check(start.plusMonths(4), "4 months")
		check(start.plusYears(2), "2 years")
		check(start.plusWeeks(3), "21 days") // no weeks field, so represented as days
		check(start.plusYears(1).plusDays(4).plusHours(3).plusMinutes(2).plusSeconds(1), "1 year, 4 days and 3 hours")
		check(start.plusDays(4).plusHours(3).plusMinutes(2).plusSeconds(1), "4 days and 3 hours")
		check(start.plusDays(4).plusMinutes(2).plusSeconds(1), "4 days")
		check(start.plusHours(3).plusMinutes(2).plusSeconds(1), "3 hours and 2 minutes")
		check(start.plusHours(1).plusMinutes(2).plusSeconds(1), "1 hour and 2 minutes")
		check(start.plusMinutes(2).plusSeconds(1), "2 minutes and 1 second")
		
		check(start.minusHours(2).minusMinutes(25), "2 hours and 25 minutes ago")
		check(start, "0 seconds ago")
	}
	
	@Test def daylightSavingOverlap {
		// 1am - 2am doesn't exist due to going an hour ahead, so midnight-4am is 3 hours long
		val start = new DateTime(2012, 3, 25, 0, 0, 0)
		val end = new DateTime(2012, 3, 25, 4, 0, 0)
		check(start, end, "3 hours")
	}
	
	def check(start:DateTime, end:DateTime, expected:String) { 
		DurationFormatter.format(start, end) should be (expected)
	}
	def check(end:DateTime, expected:String)(implicit start:DateTime):Unit = check(start,end,expected)
}