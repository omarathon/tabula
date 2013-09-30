package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants

class TermServiceTest extends TestBase{

	@Test
	def canGetAcademicWeeksBetweenDates(){
		val service = new TermServiceImpl
		val now = new DateTime(2013, DateTimeConstants.SEPTEMBER, 27, 0, 0, 0, 0)
		val weeks = service.getAcademicWeeksBetween(now.minusYears(1), now.plusYears(1))
		weeks.size should be(53 * 2) // 2 year span
		weeks.head._2 should be(1) // list starts with term1 week 1
		// it would be nice to make some more precise assertions about what the academic year is, but I'm
		// not sure how to do that in a way which wille work reliably when the various different year starts roll over
		weeks.head._1.startYear should be < (weeks.last._1.startYear)
	}

}
