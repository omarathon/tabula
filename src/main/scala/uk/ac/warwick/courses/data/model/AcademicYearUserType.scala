package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.AcademicYear

/**
 * Stores an AcademicYear as an integer (which is the 4-digit start year)
 */
final class AcademicYearUserType extends AbstractIntegerUserType[AcademicYear] {
  def convertToObject(input: java.lang.Integer) = new AcademicYear(input)
  def convertToInteger(year: AcademicYear) = year.startYear
}