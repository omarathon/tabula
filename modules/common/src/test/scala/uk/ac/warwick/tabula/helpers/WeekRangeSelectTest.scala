package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.termdates.TermFactoryImpl
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

class WeekRangesSelectTest extends TestBase {

	val termFactory = new TermFactoryImpl

	@Test def firstTermNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessByDate(DateTime.now))
		select.termFactory = termFactory
		select.weekRange.termFactory = termFactory

		select.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be ( Seq(
					EventWeek(1,1), EventWeek(2,2), EventWeek(3,3), EventWeek(4,4), EventWeek(5,5), EventWeek(6,6), EventWeek(7,7), EventWeek(8,8), EventWeek(9,9), EventWeek(10,10)
		))

		select.format(Seq(WeekRange(1, 5), WeekRange(7, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3), EventWeek(4,4), EventWeek(5,5), EventWeek(7,7), EventWeek(8,8), EventWeek(9,9), EventWeek(10,10)
		))

		select.format(Seq(WeekRange(1, 3), WeekRange(15, 18)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3)
		))

	}

	@Test def secondTermNumbering = withFakeTime(new DateTime(2012, 2, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessByDate(DateTime.now))
		select.termFactory = termFactory
		select.weekRange.termFactory = termFactory

		select.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be ( Seq() )

		select.format(Seq(WeekRange(1, 3), WeekRange(15, 18)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be( Seq(
			EventWeek(1, 15), EventWeek(2, 16), EventWeek(3, 17), EventWeek(4, 18)
		))
	}


	@Test def firstCumulativeTermNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val select = new WeekRangeSelectFormatter(AcademicYear.guessByDate(DateTime.now))
		select.termFactory = termFactory
		select.weekRange.termFactory = termFactory

		select.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be( Seq(
			EventWeek(1,1), EventWeek(2,2), EventWeek(3,3), EventWeek(4,4), EventWeek(5,5), EventWeek(6,6), EventWeek(7,7), EventWeek(8,8), EventWeek(9,9), EventWeek(10,10)
		))

	}


	/*
	@Test def academicWeekNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {

	}

	@Test def noWeekNumbers = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {

	*/

}
