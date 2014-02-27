package uk.ac.warwick.tabula.data.model.attendance

import uk.ac.warwick.tabula.TestBase

class MonitoringPointTest extends TestBase {

	@Test def includesWeek {
		val point = new MonitoringPoint
		point.validFromWeek = 2
		point.requiredFromWeek = 4
		point.includesWeek(1) should be (false)
		point.includesWeek(2) should be (true)
		point.includesWeek(3) should be (true)
		point.includesWeek(4) should be (true)
		point.includesWeek(5) should be (false)
	}

}
