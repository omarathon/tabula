package uk.ac.warwick.courses
import org.junit.Test
import org.joda.time.DateTime

class AcademicYearTest extends TestBase {
	@Test def year {
		AcademicYear.guessByDate(dateTime(2010,11)).startYear should be(2010)
		AcademicYear.guessByDate(dateTime(2010,05)).startYear should be(2009)
	}
	
	@Test def strings {
		new AcademicYear(2011).toString should be ("11/12")
		new AcademicYear(1999).toString should be ("99/00")
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def tooHigh {
		new AcademicYear(9999)
	}
	
	@Test(expected=classOf[IllegalArgumentException]) def tooLow {
		new AcademicYear(999)
	}
	
}