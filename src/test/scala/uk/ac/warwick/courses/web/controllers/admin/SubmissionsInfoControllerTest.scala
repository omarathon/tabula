package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class SubmissionsInfoControllerTest extends TestBase {
	@Test def timeFormat = {
		val controller = new SubmissionsInfoController()
		val lovelyDate = DateTime.parse("2012-08-15T11:20+0100")
		controller.format(lovelyDate) should be ("2012-08-15T11:20:00+01:00")
	}
}