package uk.ac.warwick.tabula.commands.cm2

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.MarkingSummaryCommand.MarkingSummaryMarkerInformation

object CourseworkMarkerHomepageCommand {
	type Command = Appliable[MarkingSummaryMarkerInformation]

	def apply(user: CurrentUser): Command = MarkingSummaryCommand.apply(user)
}

