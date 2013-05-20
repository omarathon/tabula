package uk.ac.warwick.tabula.helpers

import uk.ac.warwick.util.termdates.TermFactoryImpl
import org.junit.Test
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

class WeekRangesFormatterTest extends TestBase {
	
	val termFactory = new TermFactoryImpl
	
	@Test def itWorks = withFakeTime(new DateTime(2012,4,12, 13,36,44)) {
		val formatter = new WeekRangesFormatter(AcademicYear.guessByDate(DateTime.now))
		formatter.termFactory = termFactory
		
		formatter.format(Seq(WeekRange(1, 10)), DayOfWeek.Tuesday, WeekRange.NumberingSystem.Term) should be ("Term 1, weeks 1-10")
	}

}