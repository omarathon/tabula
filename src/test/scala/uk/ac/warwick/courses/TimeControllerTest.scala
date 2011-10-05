package uk.ac.warwick.courses

import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit

class TimeControllerTest extends JUnitSuite with ShouldMatchersForJUnit {
	@Test def works {
		controller.showTime.getModel.get("timeWelcome") should be ("Yo")
	} 

	@Test def something {
		controller should not be(null)
	}
	
	def controller = new TimeController {
		timeWelcome = "Yo"
	}
	
}
