package uk.ac.warwick.tabula.commands.cm2

import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.MarkingSummaryCommand.MarkingSummaryMarkerInformation
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object CourseworkMarkerHomepageCommand {
  type Command = Appliable[MarkingSummaryMarkerInformation]

  def apply(user: CurrentUser, academicYear: AcademicYear): Command = MarkingSummaryCommand(user, academicYear)
}

