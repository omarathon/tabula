package uk.ac.warwick.courses.web.controllers.sysadmin

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses.Features
import java.util.Properties

class FeaturesControllerTest extends TestBase {
	@Test def access {
		val features = new Features(new Properties)
		val controller = new FeaturesController(features)
		
		controller.currentValues should not be ('empty)
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)
		
		features.emailStudents = true
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (true)
	}
}