package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.permissions.Permissions

class PermissionConverterTest extends TestBase {
	
	val converter = new PermissionConverter
	
	@Test def validInput {
		converter.convertRight("Module.Create") should be (Permissions.Module.Create)
	}
	
	@Test def invalidInput {
		converter.convertRight("20X6") should be (null)
		converter.convertRight("") should be (null)
	}
	
	@Test def formatting {
		converter.convertLeft(Permissions.Module.Create) should be ("Module.Create")
		converter.convertLeft(null) should be (null)
	}

}