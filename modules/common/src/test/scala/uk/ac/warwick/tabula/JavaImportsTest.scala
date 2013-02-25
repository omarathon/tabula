package uk.ac.warwick.tabula

class JavaImportsTest extends TestBase with JavaImports {
	
	@Test def jSet() {
		JSet("a", "b", "b").size() should be (2)
	}
	
	@Test def jBoolean() {
		ToJBoolean(Some(true)) should be (java.lang.Boolean.TRUE)
		ToJBoolean(Some(false)) should be (java.lang.Boolean.FALSE)
		ToJBoolean(None) should be (null)
	}
	
	@Test def jInteger() {
		ToJInteger(Some(1)) should be (1)
		ToJInteger(Some(0)) should be (0)
		ToJInteger(None) should be (null)
	}

}