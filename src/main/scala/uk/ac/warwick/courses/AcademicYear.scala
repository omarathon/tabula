package uk.ac.warwick.courses
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import java.beans.PropertyEditorSupport

/**
 * Represents a particular academic year. Traditionally they are displayed as
 * "99/00" or "11/12" but we just store the first year as a 4-digit number.
 * toString() returns the traditional format.
 */
case class AcademicYear(val startYear:Int) {
	val endYear = startYear+1
	if (endYear > 9999 || startYear < 1000) throw new IllegalArgumentException()
	
	override def toString = "%s/%s".format(toDigits(startYear), toDigits(endYear))
	private def toDigits(year:Int) = year.toString.substring(2)
	
	// properties for binding to dropdown box
	def getStoreValue = startYear
	def getLabel = toString
	
	def previous = new AcademicYear(startYear-1)
	def next = new AcademicYear(startYear+1)
	
	def -(i:Int) = new AcademicYear(startYear-i)
	def +(i:Int) = new AcademicYear(startYear+i)
	
	/**
	 * Returns a sequence of AcademicYears, in order, starting
	 * the given number of years before this year, and ending
	 * the given number of years after, inclusive. The length
	 * will be 1 + yearsBefore + yearsAfter. If both are 0, then
	 * it will have a single element containing this year.
	 */
	def yearsSurrounding(yearsBefore:Int, yearsAfter:Int) : Seq[AcademicYear] = {
		assert(yearsBefore >= 0)
		assert(yearsAfter >= 0)
		val length = 1 + yearsBefore + yearsAfter
		val first = (this - yearsBefore)
		Iterable.iterate(first, length) { y => y.next }.toSeq
	}
}

/**
 * Stores academic year as the 4-digit starting year.
 */
class AcademicYearEditor extends PropertyEditorSupport {
	override def getAsText = getValue() match {
		case year:AcademicYear => year.startYear.toString
		case _ => ""
	}
	
	override def setAsText(year:String) = try {
		setValue(new AcademicYear(year.toInt))
	} catch {
		case e:NumberFormatException => setValue(null)
	}
}

object AcademicYear {
	
	private val SitsPattern = """(\d{2})/(\d{2})""".r
	/**
	 * We're only dealing with current years, not DOBs or anything, so can afford
	 * to make the century break large. I don't think there is even any module data in SITS
	 * from before 2004, so could even do without this check.
	 * 
	 * Anyway, this will only break near the year 2090.
	 */
	private val CenturyBreak = 90
	
	def parse(string:String) = string match {
		case SitsPattern(year1, year2) => AcademicYear(parseTwoDigits(year1))
		case _ => throw new IllegalArgumentException("Did not match YY/YY: " + string)
	}
	
	/**
	 * Academic years in SITS are only 2 digits so we need to be able to parse them.
	 * Assume that it's almost always a 20XX date.
	 */
	private def parseTwoDigits(twoDigitYear:String) = twoDigitYear.toInt match {
		case y if y > CenturyBreak => 1900 + y
		case y => 2000 + y
	}
	
	def guessByDate(now:DateTime) = {
		if (now.getMonthOfYear() >= AUGUST) {
			new AcademicYear(now.getYear())
		} else {
			new AcademicYear(now.getYear()-1)
		}
	}
}