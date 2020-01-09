package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.system.TwoWayConverter

class FeedbackIdConverter extends TwoWayConverter[String, Feedback] {

  @Autowired var service: FeedbackDao = _

  override def convertRight(id: String):Feedback = service.getFeedback(id).orNull

  override def convertLeft(feedback: Feedback): String = (Option(feedback).map(_.id)).orNull

}
