package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.DegreeType._

class DegreeTypeTest extends TestBase {
  
	@Test def convertToObject() {
		val t = new DegreeTypeUserType
		t.convertToObject("UG") should be (Undergraduate)
		t.convertToObject("PG") should be (Postgraduate)
		t.convertToObject("IS") should be (InService)
		t.convertToObject("PGCE") should be (PGCE)
		evaluating { t.convertToObject("Q") } should produce [IllegalArgumentException]
	}
  
	@Test def convertToValue() {
		val t = new DegreeTypeUserType
		t.convertToValue(Undergraduate) should be ("UG")
		t.convertToValue(Postgraduate) should be ("PG")
		t.convertToValue(InService) should be ("IS")
		t.convertToValue(PGCE) should be ("PGCE")
	}

}