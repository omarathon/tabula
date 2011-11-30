package uk.ac.warwick.courses
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._

class AcademicYear(val startYear:Int) {
	val endYear = startYear+1
	if (endYear > 9999 || startYear < 1000) throw new IllegalArgumentException()
	
	override def toString = "%s/%s".format(toDigits(startYear), toDigits(endYear))
	private def toDigits(year:Int) = year.toString.substring(2)
}

object AcademicYear {
	def guessByDate(now:DateTime) = {
		if (now.getMonthOfYear() >= AUGUST) {
			new AcademicYear(now.getYear())
		} else {
			new AcademicYear(now.getYear() - 1)
		}
	}
}