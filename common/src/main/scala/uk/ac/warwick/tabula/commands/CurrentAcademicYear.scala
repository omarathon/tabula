package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.AcademicYear

trait CurrentAcademicYear extends HasAcademicYear {

	var academicYear: AcademicYear = AcademicYear.now()

	def getAcademicYearString: String = Option(academicYear).map(_.toString).getOrElse("")

}
