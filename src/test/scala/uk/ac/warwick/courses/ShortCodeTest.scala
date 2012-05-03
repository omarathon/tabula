package uk.ac.warwick.courses

import org.junit.Test

class ShortCodeTest extends TestBase {
	@Test def randomShortCode {
		ShortCode.random() should fullyMatch regex """[a-zA-Z0-9]{5}"""
	}
}