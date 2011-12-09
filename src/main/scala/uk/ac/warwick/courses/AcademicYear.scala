package uk.ac.warwick.courses
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants._
import java.beans.PropertyEditorSupport

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
	
	def guessByDate(now:DateTime) = {
		if (now.getMonthOfYear() >= AUGUST) {
			new AcademicYear(now.getYear() + 1)
		} else {
			new AcademicYear(now.getYear())
		}
	}
}