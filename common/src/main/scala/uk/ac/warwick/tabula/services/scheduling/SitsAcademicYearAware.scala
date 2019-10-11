package uk.ac.warwick.tabula.services.scheduling

import uk.ac.warwick.tabula.AcademicYear

trait SitsAcademicYearAware {
  def getCurrentSitsAcademicYear: AcademicYear =
    AcademicYear.now()
}
