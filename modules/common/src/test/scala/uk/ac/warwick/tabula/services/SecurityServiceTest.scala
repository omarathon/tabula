package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula._
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.hamcrest.Matchers._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.tabula.roles._
import uk.ac.warwick.tabula.data.model.Assignment

class SecurityServiceTest extends TestBase with Mockito {
	
	val user = new User("cusebr")
	user.setIsLoggedIn(true)
	user.setFoundUser(true)
	
	@Test def godMode {
		val securityService = new SecurityService

		val currentUser = new CurrentUser(user, user, god=true)
		
		securityService.can(currentUser, Permissions.GodMode) should be (true)
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, new Department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.PersonalTutor.Create, null) should be (true)
		securityService.can(currentUser, null) should be (true)
		securityService.can(currentUser, null, null) should be (true)
	}
	
	@Test def explicitPermission {
		val securityService = new SecurityService
		
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val roleService = mock[RoleService]
		roleService.getExplicitPermissionsFor(currentUser, null) returns (Map(
				Permissions.UserPicker -> None,
				Permissions.ImportSystemData -> None
		))
		roleService.getExplicitPermissionsFor(currentUser, department) returns (Map(
				Permissions.Department.ManageDisplaySettings -> Some(department)
		))
				
		securityService.roleService = roleService
		
		securityService.can(currentUser, Permissions.GodMode) should be (false)
		securityService.can(currentUser, Permissions.Module.Create, department) should be (false)
		securityService.can(currentUser, Permissions.UserPicker) should be (true)
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, department) should be (true)
	}
	
	@Test def role {
		val securityService = new SecurityService
		
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val roleService = mock[RoleService]
		roleService.getRolesFor(currentUser, null) returns (Stream(
			Sysadmin()
		))
		roleService.getRolesFor(currentUser, department) returns (Stream(
			Sysadmin(),
			DepartmentalAdministrator(department)
		))
				
		securityService.roleService = roleService
		
		securityService.can(currentUser, Permissions.UserPicker) should be (false)
		securityService.can(currentUser, Permissions.GodMode) should be (true)
		securityService.can(currentUser, Permissions.Department.ManageDisplaySettings, department) should be (true)
		// Global perm
		securityService.can(currentUser, Permissions.Module.Create, department) should be (true)
		securityService.can(currentUser, Permissions.Profiles.PersonalTutor.Update, null) should be (false)
	}
	
	@Test def globalPermission {
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> None
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, null
		) should be (securityService.Continue)
	}
	
	@Test def exactScopeMatch {
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> Some(department)
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Module
		) should be (securityService.Continue)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
	}
	
	@Test def scopelessMatch {
		val department = new Department
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> None
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, department
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, department
		) should be (securityService.Continue)
	}
	
	@Test def higherScopeMatch {
		val module = new Module
		val department = new Department
		module.department = department
		
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.Department.DownloadFeedbackReport -> Some(department)
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, module
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.DownloadFeedbackReport, new Assignment
		) should be (securityService.Continue)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.Department.ManageDisplaySettings, module
		) should be (securityService.Continue)
	}
	
	@Test def scopelessPermission {
		val currentUser = new CurrentUser(user, user)
		
		val securityService = new SecurityService
		val permissions: Map[Permission, Option[PermissionsTarget]] = Map(
				Permissions.GodMode -> None
		)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.GodMode, null
		) should be (securityService.Allow)
		
		securityService.checkPermissions(
				permissions, currentUser, Permissions.ImportSystemData, null
		) should be (securityService.Continue)
	}
	
	/*
	 * Just testing a compiler warning that scared me, about it
	 * mismatching case classes that use inheritence. But it's
	 * only when the superclass is a case class, which we don't do. 
	 * I'll leave this in though!
	 */
	@Test def caseMatching {
	  val action = Permissions.GodMode
	  
	  (action:Any) match {
	    case Permissions.GodMode => {}
	    case _ => fail("Should have matched god mode")
	  }
	}
			
}