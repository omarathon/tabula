package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.Promises._

class FeedbackIdConverter extends TwoWayConverter[String, Feedback] {

	val service = promise { Wire[FeedbackDao] }

	override def convertRight(id: String) = service.get.getFeedback(id).orNull
	override def convertLeft(feedback: Feedback) = (Option(feedback) map {_.id}).orNull

}