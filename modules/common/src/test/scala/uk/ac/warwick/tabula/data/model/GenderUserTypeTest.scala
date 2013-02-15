package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Gender._

class GenderUserTypeTest extends TestBase {
  
	@Test def convertToObject() {
		val t = new GenderUserType
		t.convertToObject("M") should be (Male)
		t.convertToObject("F") should be (Female)
		t.convertToObject("N") should be (Unspecified)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}
  
	@Test def convertToValue() {
		val t = new GenderUserType
		t.convertToValue(Male) should be ("M")
		t.convertToValue(Female) should be ("F")
		t.convertToValue(Unspecified) should be ("N")
	}

}