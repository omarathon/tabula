package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services._

class UserLookupServiceTests extends TestBase with Mockito {

	@Test def swappingUserLookup {
		val lookup1 = mock[UserLookupService]
		val lookup2 = mock[UserLookupService]
		
		val swappable = new SwappableUserLookupService(lookup1)
		swappable.getUserByUserId("a")
		swappable.delegate = lookup2
		swappable.getUserByUserId("b")
		
		there was one(lookup1).getUserByUserId("a")
		there was one(lookup2).getUserByUserId("b")
	}
	
}