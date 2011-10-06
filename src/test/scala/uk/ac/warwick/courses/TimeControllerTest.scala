package uk.ac.warwick.courses

import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import uk.ac.warwick.courses.controllers._

class TimeControllerTest extends JUnitSuite with ShouldMatchersForJUnit with Mocking {
	@Test def works {
		controller.showTime.getModel.get("timeWelcome") should be ("Yo")
	} 
	
	def controller = new TimeController {
		timeWelcome = "Yo"
	}
	
}
