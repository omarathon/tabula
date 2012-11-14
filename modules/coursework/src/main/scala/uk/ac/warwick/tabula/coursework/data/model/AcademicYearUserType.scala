package uk.ac.warwick.tabula.coursework.data.model

import uk.ac.warwick.tabula.coursework.AcademicYear
import uk.ac.warwick.tabula.JavaImports._

/**
 * Stores an AcademicYear as an integer (which is the 4-digit start year)
 */
final class AcademicYearUserType extends AbstractIntegerUserType[AcademicYear] {
	def convertToObject(input: java.lang.Integer) = new AcademicYear(input)
	def convertToValue(year: AcademicYear) = year.startYear

	val nullValue = 0: JInteger
	val nullObject = null
}