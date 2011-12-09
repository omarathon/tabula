package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.AcademicYear

final class AcademicYearUserType extends AbstractStringUserType[AcademicYear] {
	
  def convertToObject(input: String) = try {
	  new AcademicYear(input.toInt)
  } catch {
	  case e:NumberFormatException => null 
  }
  
  def convertToString(year: AcademicYear) = year.startYear.toString
}