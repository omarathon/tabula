package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import util.Random

class MarkerFeedbackTests extends TestBase {

	@Test def fields() {

		val random = new Random
		val feedback = new Feedback(universityId = idFormat(1))

		val firstMarkerFeedback = new MarkerFeedback()
		val mark1 = random.nextInt(101)
		firstMarkerFeedback.mark = Some(mark1)
		firstMarkerFeedback.mark.get should be (mark1)


		val secondMarkerFeedback = new MarkerFeedback()
		val mark2 = random.nextInt(101)
		secondMarkerFeedback.mark = Some(mark2)
		secondMarkerFeedback.mark.get should be (mark2)

		feedback.addFirstMarkerFeedback(firstMarkerFeedback)
		feedback.addSecondMarkerFeedback(secondMarkerFeedback)

		firstMarkerFeedback.feedback.id should be (feedback.id)
		secondMarkerFeedback.id should be (feedback.id)
		feedback.firstMarkerFeedback.id should be (firstMarkerFeedback.id)
		feedback.secondMarkerFeedback.id should be (secondMarkerFeedback.id)

	}


	/** Zero-pad integer to a 7 digit string */
	def idFormat(i:Int) = "%07d" format i
}
