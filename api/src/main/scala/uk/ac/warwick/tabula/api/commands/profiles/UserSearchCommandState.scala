package uk.ac.warwick.tabula.api.commands.profiles

import uk.ac.warwick.tabula.AcademicYear

trait UserSearchCommandState {
  def academicYear: AcademicYear
}