package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.helpers.cm2.{SubmissionAndFeedbackInfoFilter, SubmissionAndFeedbackInfoFilters}
import uk.ac.warwick.tabula.system.TwoWayConverter

class SubmissionAndFeedbackInfoFilterConverter extends TwoWayConverter[String, SubmissionAndFeedbackInfoFilter] {

	override def convertRight(source: String): SubmissionAndFeedbackInfoFilter = SubmissionAndFeedbackInfoFilters.of(source)

	override def convertLeft(source: SubmissionAndFeedbackInfoFilter): String = Option(source).map {
		_.getName
	}.orNull

}
