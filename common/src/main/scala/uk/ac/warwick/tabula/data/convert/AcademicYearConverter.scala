package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

/**
 * Stores academic year as the 4-digit starting year.
 */
class AcademicYearConverter extends TwoWayConverter[String, AcademicYear] {

	override def convertRight(year: String): AcademicYear =
		if ("current" == year || "sits" == year) AcademicYear.now()
		else if (year.hasText)
			try { AcademicYear.parse(year) } catch {
				case e: IllegalArgumentException =>
					try { AcademicYear(year.toInt) } catch { case _: NumberFormatException => null }
			}
		else null

	override def convertLeft(year: AcademicYear): String = Option(year) match {
		case Some(y) => y.startYear.toString
		case None => null
	}

}