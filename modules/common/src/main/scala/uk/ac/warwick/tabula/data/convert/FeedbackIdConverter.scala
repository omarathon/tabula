package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.system.TwoWayConverter

class FeedbackIdConverter extends TwoWayConverter[String, Feedback] {

	var service = Wire[FeedbackDao]

	override def convertRight(id: String) = service.getFeedback(id).orNull
	override def convertLeft(feedback: Feedback) = (Option(feedback) map {_.id}).orNull

}