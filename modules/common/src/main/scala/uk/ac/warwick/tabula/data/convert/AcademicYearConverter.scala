package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.TwoWayConverter

/**
 * Stores academic year as the 4-digit starting year.
 */
class AcademicYearConverter extends TwoWayConverter[String, AcademicYear] {
  	
	override def convertRight(year: String) =
		if (year.hasText) try { new AcademicYear(year.toInt) } catch { case e: NumberFormatException => null }
		else null
		
	override def convertLeft(year: AcademicYear) = Option(year) match {
		case Some(year) => year.startYear.toString
		case None => null
	}

}