package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.Features
import java.util.Properties
import uk.ac.warwick.util.queue.activemq.ActiveMQQueueProvider
import org.scalatest.mock.MockitoSugar
import uk.ac.warwick.util.queue.Queue

class FeaturesControllerTest extends TestBase with MockitoSugar {
	@Test def access {
		val features = Features.empty
		val controller = new FeaturesController
		controller.features = features
		controller.queue = mock[Queue]
		
		controller.afterPropertiesSet
		
		controller.currentValues should not be ('empty)
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)
		
		features.emailStudents = true
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (true)
		
		controller.update("emailStudents", false)
		
		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)
	}
}