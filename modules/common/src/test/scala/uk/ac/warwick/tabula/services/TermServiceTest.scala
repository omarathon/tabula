package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{AcademicYear, TestBase}
import org.joda.time.{Interval, DateTime}
import org.joda.time.DateTimeConstants._

class TermServiceTest extends TestBase {
		@Test
		def canGetAcademicWeeksBetweenDates(){
			val service = new TermServiceImpl
			val weeks = service.getAcademicWeeksBetween( new DateTime(2012, OCTOBER, 1, 0, 0, 0, 0), new DateTime(2014, SEPTEMBER, 29, 0, 0, 0, 0))
			weeks.size should be(104) // 2 years, week 53 omitted
			weeks.head._2 should be(1) // list starts with term1 week 1
			weeks.head._1 should be (AcademicYear(2012))
			weeks.last._1 should be (AcademicYear(2013))
			weeks.last._2 should be (52)
		}

	@Test
	def zeroLengthIntervalGetsOneWeek() {
		val service = new TermServiceImpl
		val beforeTermStarts = new DateTime(2013, SEPTEMBER, 29, 0, 0, 0, 0)
		val firstDayOfTerm = new DateTime(2013, SEPTEMBER, 30, 0, 0, 0, 0) // Exactly the same instant as the start of the first day of term
		val secondDayOfTerm = new DateTime(2013, OCTOBER, 1, 0, 0, 0, 0)

		val dates = Seq(beforeTermStarts, firstDayOfTerm, secondDayOfTerm)
		dates.foreach {
			date =>
				val interval = new Interval(date, date)
				val week = service.getAcademicWeeksBetween(interval.getStart, interval.getEnd)
				withClue(s"Interval $interval did not return a single week") {
					week.size should be(1)
				}
		}
	}

	@Test
	def getAcademicWeeksForArbitraryInterval(){
		val start = new DateTime(2013, OCTOBER, 1, 0, 0, 0, 0)
		val end: DateTime = new DateTime(2013, NOVEMBER, 1, 0, 0, 0, 0)

		val weeks = new TermServiceImpl().getAcademicWeeksBetween(start, end)
		weeks.size should be (5)

		weeks.head._3.contains(start) should be (true)
		weeks.last._3.contains(end) should be (true)

	}

}
