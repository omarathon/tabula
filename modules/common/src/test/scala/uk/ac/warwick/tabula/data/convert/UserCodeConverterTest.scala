package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.userlookup.User
import org.junit.Before
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.services.SwappableUserLookupService
import uk.ac.warwick.tabula.services.UserLookupServiceImpl

class UserCodeConverterTest extends AppContextTestBase with Mockito {
	
	val converter = new UserCodeConverter
	var userLookup: MockUserLookup = _
    
  @Before def getUserLookup {
		// We can't just Autowire this because it has autowire-candidate="false"
		userLookup = Wire[SwappableUserLookupService].delegate.asInstanceOf[UserLookupServiceImpl].delegate.asInstanceOf[MockUserLookup]
		userLookup.registerUsers("cuscav")
	}
	
	@Test def validInput {		
		converter.convertRight("cuscav").isFoundUser() should be (true)
	}
	
	@Test def invalidInput {
		converter.convertRight("20X6").isFoundUser() should be (false)
	}
	
	@Test def formatting {
		converter.convertLeft(new User("cuscav")) should be ("cuscav")
		converter.convertLeft(null) should be (null)
	}

}