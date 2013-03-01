package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions.Permissions

class ControllerMethodsTest extends TestBase with ControllerMethods with Mockito {
	
	var securityService = mock[SecurityService]
	val user = {
		val u = new User("cuscav")
		u.setIsLoggedIn(true)
		u.setFoundUser(true)
		
		new CurrentUser(u, u)
	}
	
	val dept = Fixtures.department("in", "IT Services")
	dept.id = "dept"
	
	abstract class TestCommand extends Command[Boolean] {
		def describe(d:Description) {}
		def applyInternal = true
	}
	
	case class BasicCommand() extends TestCommand {
		PermissionCheck(Permissions.Module.Create, dept)
	}
	
	@Test def restricted {
		securityService.can(user, Permissions.Module.Create, dept) returns (false)
		
		restricted(BasicCommand()) should be (None)
		
		securityService.can(user, Permissions.Module.Create, dept) returns (true)
		
		restricted(BasicCommand()) should be ('defined)
	}
	
	@Test def restrictedBy {
		securityService.can(user, Permissions.Module.Create, dept) returns (false)
		
		restrictedBy(true)(BasicCommand()) should be (None)
		
		// Restriction doesn't apply because predicate returns false
		restrictedBy(false)(BasicCommand()) should be ('defined)
		
		securityService.can(user, Permissions.Module.Create, dept) returns (true)
		
		restrictedBy(true)(BasicCommand()) should be ('defined)
		restrictedBy(false)(BasicCommand()) should be ('defined)
	}

}