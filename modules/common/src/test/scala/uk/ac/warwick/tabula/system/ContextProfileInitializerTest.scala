package uk.ac.warwick.tabula.system
import org.junit.Test
import uk.ac.warwick.tabula.TestBase


import org.springframework.mock.env.MockPropertySource

class ContextProfileInitializerTest extends TestBase {
	@Test def devWeb {
		val initializer = new ContextProfileInitializer
		initializer.testConfig = mockProperties(
			"spring.profiles.active" -> "dev",
			"scheduling.enabled" -> "true"
		)
		val profiles = initializer.resolve
		profiles should (
				have size (3) and
				contain ("dev") and
				contain ("web") and
				contain ("scheduling")
		)
	}
	
	@Test def prodNoWeb {
		val initializer = new ContextProfileInitializer
		initializer.testConfig = mockProperties(
			"spring.profiles.active" -> "production",
			"web.enabled" -> "false",
			"api.enabled" -> "false"
		)
		val profiles = initializer.resolve
		profiles should (
				have size (1) and
				contain ("production")
		)
	}
	
	def mockProperties(pairs:(String, String)*) =
		new MockPropertySource {
			for ((key, value) <- pairs) {
				setProperty(key, value)
			}
		}
	
}