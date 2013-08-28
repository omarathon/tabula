package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{AcademicYear, Mockito, TestBase}
import uk.ac.warwick.util.termdates.TermFactory
import org.joda.time._
import uk.ac.warwick.tabula.JavaImports.{JArrayList,JInteger}
import uk.ac.warwick.util.collections.{Pair=>WPair}
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import scala.Some

class TermAwareWeekRangeToDateConversionServiceTest extends TestBase with Mockito{

	val localNow = LocalDateTime.now
	val dtNow = localNow.toDateTime

  val localCurrentYear = AcademicYear.guessByDate(localNow.toDateTime)

	val week1:WeekRange.Week = 1
  val week1Interval = new Interval(dtNow.withDayOfWeek(DateTimeConstants.MONDAY).withTimeAtStartOfDay(),
		                               dtNow.withDayOfWeek(DateTimeConstants.MONDAY).withTimeAtStartOfDay().plusDays(7))
	val mockTf = mock[TermFactory]
	val converter = new TermAwareWeekToDateConverterComponent with TermFactoryComponent{
		var termFactory = mockTf
	}.weekToDateConverter

	mockTf.getAcademicWeeksForYear(localCurrentYear.dateInTermOne) returns JArrayList(
	  // week 1 includes localNow
		WPair.of(JInteger(Some(week1)), week1Interval),
		// week 2 is after localNow
		WPair.of(JInteger(Some(2)), new Interval(week1Interval.getEnd, week1Interval.getEnd.plusDays(7)))
	)

	@Test
	def canGetWeekContainingDate(){
		converter.getWeekContainingDate(localNow.toLocalDate) should be(Some(week1))
	}

	@Test
  def GetWeekContaingDateReturnsNoneIfNoMatch(){
		converter.getWeekContainingDate(localNow.minusDays(7).toLocalDate) should be(None)
	}

	@Test
	def intersectsWeekReturnsTrueIfWeeksIntersect(){
		val whollyContained = new Interval(dtNow, dtNow.plusDays(1))
		val overlapsBothEnds = new Interval(dtNow.minusDays(5), dtNow.plusDays(10))
		val overlapsStart = new Interval(dtNow.minusDays(5), dtNow.plusDays(1))
		val overlapsEnd = new Interval(dtNow.plusDays(2), dtNow.plusDays(10))

		converter.intersectsWeek(whollyContained,week1,localCurrentYear) should be(true)
		converter.intersectsWeek(overlapsBothEnds, week1, localCurrentYear) should be (true)
		converter.intersectsWeek(overlapsStart, week1, localCurrentYear) should be (true)
		converter.intersectsWeek(overlapsEnd, week1, localCurrentYear) should be (true)
	}

	@Test
	def intersectsWeekReturnsFalseIfWeeksDontIntersect(){
		val before= new Interval(dtNow.minusDays(6), dtNow.minusDays(5))
		val after = new Interval(dtNow.plusDays(10), dtNow.plusDays(15))
		converter.intersectsWeek(before,week1,localCurrentYear) should be(false)
		converter.intersectsWeek(after, week1, localCurrentYear) should be (false)
	}

	@Test
  def toLocalDateTimeWorks(){
		val halfTwo= new LocalTime(14,30,0)
		val dateTime = converter.toLocalDatetime(week1,DayOfWeek.Tuesday,halfTwo,localCurrentYear).get

		week1Interval.contains(dateTime.toDateTime()) should be (true)
		dateTime.getDayOfWeek should be(DateTimeConstants.TUESDAY)
		dateTime.toLocalTime should be(halfTwo)
	}

	@Test
	def toLocalDateTimeReturnsNoneIfNoWeekFound(){
		val halfTwo= new LocalTime(14,30,0)
		val weekThatDoesntExist = 54
		val dateTime = converter.toLocalDatetime(weekThatDoesntExist,DayOfWeek.Tuesday,halfTwo,localCurrentYear)
		dateTime should be(None)
	}

}
