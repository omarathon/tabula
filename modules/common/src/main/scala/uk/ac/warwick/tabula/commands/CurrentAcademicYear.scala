package uk.ac.warwick.tabula.commands

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear

trait CurrentAcademicYear {

	var academicYear: AcademicYear = AcademicYear.guessByDate(new DateTime)

	def getAcademicYearString = Option(academicYear).map(_.toString).getOrElse("")

}
