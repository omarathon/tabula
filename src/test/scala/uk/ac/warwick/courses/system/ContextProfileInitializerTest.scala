package uk.ac.warwick.courses.system
import uk.ac.warwick.courses.TestBase
import org.junit.Test

class ContextProfileInitializerTest extends TestBase {
	@Test def mappings {
		val initializer = new ContextProfileInitializer
		initializer.resolve("dev,scheduling") should be (Array("dev", "scheduling"))
	}
}