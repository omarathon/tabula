package uk.ac.warwick.courses.web.controllers.sysadmin

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import uk.ac.warwick.courses.Features
import java.util.Properties

class FeaturesControllerTest extends TestBase {
	@Test def access {
		val features = Features.fromProperties(new Properties)
		val controller = new FeaturesController
		controller.features = features
		controller.afterPropertiesSet
		
		controller.currentValues should not be ('empty)
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)
		
		features.emailStudents = true
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (true)
		
		controller.update("emailStudents", false)
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)
	}
}