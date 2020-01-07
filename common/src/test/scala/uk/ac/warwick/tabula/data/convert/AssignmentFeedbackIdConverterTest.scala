package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.Feedback

class FeedbackIdConverterTest extends TestBase with Mockito {

  val converter = new FeedbackIdConverter
  var service: FeedbackDao = mock[FeedbackDao]
  converter.service = service

  @Test def validInput(): Unit = {
    val feedback = new Feedback
    feedback.id = "steve"

    service.getFeedback("steve") returns Some(feedback)

    converter.convertRight("steve") should be(feedback)
  }

  @Test def invalidInput(): Unit = {
    service.getFeedback("20X6") returns None
    converter.convertRight("20X6") should be(null)
  }

  @Test def formatting(): Unit = {
    val feedback = new Feedback
    feedback.id = "steve"

    converter.convertLeft(feedback) should be("steve")
    converter.convertLeft(null) should be(null)
  }

}