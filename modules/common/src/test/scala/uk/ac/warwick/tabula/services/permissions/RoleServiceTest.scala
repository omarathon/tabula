package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.Fixtures

class RoleServiceTest extends TestBase with Mockito {
	
	@Test def lazyEvaluation() = withUser("cuscav", "0672089") {
		val provider1 = mock[ScopelessRoleProvider]
		val provider2 = mock[ScopelessRoleProvider]
			
		when(provider1.getRolesFor(currentUser)) thenReturn(Seq(Sysadmin()))
		when(provider2.getRolesFor(currentUser)) thenThrow(classOf[RuntimeException])
				
		val service = new RoleService()
		service.providers = Array(provider1, provider2)
		
		val isSysadminRole = service.getRolesFor(currentUser, null) exists { _ == Sysadmin() }
		
		there was one(provider1).getRolesFor(currentUser)
		there were no(provider2).getRolesFor(currentUser)
		
		isSysadminRole should be (true)
	}

}