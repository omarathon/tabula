package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.TestBase
import org.junit.Test

class ModuleTest extends TestBase {
	@Test def stripCats {
		Module.stripCats("md101-15") should be ("md101")
		Module.stripCats("md105-5") should be ("md105")
		intercept[IllegalArgumentException] { Module.stripCats("md105") }
	}
}