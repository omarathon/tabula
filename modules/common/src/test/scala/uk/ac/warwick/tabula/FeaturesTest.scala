package uk.ac.warwick.tabula

import java.util.Properties
import org.junit.Test
import java.lang.Boolean
import org.junit.Ignore

class FeaturesTest extends TestBase {
	@Ignore @Test def validFeature {
		val props = new Properties
		props.setProperty("irrelevant.property", "who cares")
		props.setProperty("features.emailStudents", "true")
		
//		val features = Features.fromProperties(props)
//		features.emailStudents should be (Boolean.TRUE)
	}
}