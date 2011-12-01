package uk.ac.warwick.courses.web.controllers

import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.ShouldMatchersForJUnit
import uk.ac.warwick.courses.web.controllers._
import org.specs.mock.JMocker

class TimeControllerTest extends JUnitSuite with ShouldMatchersForJUnit {
	@Test def works {
		controller.showTime.toModel("timeWelcome") should be ("Yo")
	} 
	
	def controller = new TimeController {
		timeWelcome = "Yo"
	}
	
}
