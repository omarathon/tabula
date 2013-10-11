package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek
import uk.ac.warwick.tabula.services.{TermServiceImpl, TermService}
import uk.ac.warwick.util.termdates.TermFactoryImpl
import scala.collection.JavaConverters._
import uk.ac.warwick.util.termdates.Term.TermType

class SingleWeekFormatterTest extends TestBase {

	val termService = new TermServiceImpl

	@Test def termNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new SingleWeekFormatter(AcademicYear.guessByDate(DateTime.now))
		formatter.termService = termService

		formatter.format(1, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Term 1, week 1")
		formatter.format(7, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Term 1, week 7")
		formatter.format(14, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Christmas vacation, w/c Mon 2<sup>nd</sup> Jan 2012")
		formatter.format(25, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be("Easter vacation, w/c Mon 19<sup>th</sup> Mar 2012")
	}



	@Test def cumulativeTermNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new SingleWeekFormatter(AcademicYear.guessByDate(DateTime.now))
		formatter.termService = termService

		formatter.format(4, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 1, week 4")
		formatter.format(10, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 1, week 10")
		formatter.format(13, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Christmas vacation, w/c Mon 26<sup>th</sup> Dec 2011")
		formatter.format(15, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Term 2, week 11")
		formatter.format(25, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Cumulative) should be("Easter vacation, w/c Mon 19<sup>th</sup> Mar 2012")
	}


	@Test def academicWeekNumbering = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new SingleWeekFormatter(AcademicYear.guessByDate(DateTime.now))
		formatter.termService = termService

		formatter.format(1, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Week 1")
		formatter.format(7, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Week 7")
		formatter.format(16, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Week 16")
		formatter.format(52, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Week 52")
		formatter.format(14, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Week 14")
		formatter.format(25, DayOfWeek.Tuesday, WeekRange.NumberingSystem.Academic) should be("Week 25")
	}

	@Test def noWeekNumbers = withFakeTime(new DateTime(2011, 10, 12, 13, 36, 44)) {
		val formatter = new SingleWeekFormatter(AcademicYear.guessByDate(DateTime.now))
		formatter.termService = termService

		formatter.format(1, DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("w/c Mon 3<sup>rd</sup> Oct 2011")
		formatter.format(5, DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("w/c Mon 31<sup>st</sup> Oct 2011")
		formatter.format(16, DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("w/c Mon 16<sup>th</sup> Jan 2012")
		formatter.format(52, DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("w/c Mon 24<sup>th</sup> Sep 2012")
		formatter.format(25, DayOfWeek.Tuesday, WeekRange.NumberingSystem.None) should be("w/c Mon 19<sup>th</sup> Mar 2012")

	}


}
