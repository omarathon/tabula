package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase


import org.junit.Test

class ModuleTest extends TestBase {
	@Test def stripCats {
		Module.stripCats("md101-15") should be ("md101")
		Module.stripCats("md105-5") should be ("md105")
		Module.stripCats("md105-7.5") should be ("md105")
		intercept[IllegalArgumentException] { Module.stripCats("md105") }
	}
	
	@Test def extractCats {
		Module.extractCats("md101-7.5") should be (Some("7.5"))
		Module.extractCats("md101-15") should be (Some("15"))
		Module.extractCats("md101") should be (None)
	}
}