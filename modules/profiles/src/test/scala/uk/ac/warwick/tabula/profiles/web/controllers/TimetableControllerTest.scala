package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.profiles.web.views.FullCalendarEvent

class TimetableControllerTest extends TestBase{

	@Test
	def canColouriseBasedOnModule(){
		// create a bunch events spread over 3 modules, with blank colours
		val events = for {
			i<- 1 until 21;
		  j = i%3}
		yield {
			FullCalendarEvent(s"Test event $i", false, 0L,0L,"","","","","","","","","",s"module$j")
		}

		val coloured = new TimetableController().colourEvents(events)

		// every event should have a colour
		coloured.find(_.backgroundColor=="") should not be('defined)

		// there should be 3 colours in use
		coloured.map(_.backgroundColor).distinct.size should be(3)
	}

}
