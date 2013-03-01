package uk.ac.warwick.tabula.services.permissions

import org.mockito.Matchers._
import org.mockito.Mockito._
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.ExtensionManager
import uk.ac.warwick.tabula.roles.ModuleManager
import uk.ac.warwick.tabula.roles.ModuleManager
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission
import uk.ac.warwick.tabula.permissions.Permissions

class RoleServiceTest extends TestBase with Mockito {

	@Test def loggedOutRoleCheck() = withUser(null){
		val provider = new UserTypeAndDepartmentRoleProvider
		provider.getRolesFor(currentUser).size should be (0)
	}
	
	@Test def getRolesScopeless() = withUser("cuscav", "0672089") {
		val scopedProvider = mock[RoleProvider]
		val provider1 = mock[ScopelessRoleProvider]
		val provider2 = mock[ScopelessRoleProvider]
			
		when(scopedProvider.getRolesFor(isEq(currentUser), isA(classOf[PermissionsTarget]))) thenThrow(classOf[RuntimeException])
		when(provider1.getRolesFor(currentUser)) thenReturn(Seq(Sysadmin()))
		when(provider2.getRolesFor(currentUser)) thenThrow(classOf[RuntimeException])
				
		val service = new RoleServiceImpl()
		service.roleProviders = Array(scopedProvider, provider1, provider2)
		
		val isSysadminRole = service.getRolesFor(currentUser, null) exists { _ == Sysadmin() }
		
		there was one(provider1).getRolesFor(currentUser)
		there were no(provider2).getRolesFor(currentUser)
		
		isSysadminRole should be (true)
	}
	
	@Test def getRolesScoped() = withUser("cuscav", "0672089") {
		val provider1 = mock[ScopelessRoleProvider]
		val provider2 = mock[RoleProvider]
		val provider3 = mock[RoleProvider]
		val provider4 = mock[ScopelessRoleProvider]
		
		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.department = dept
		
		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider1, provider2, provider3, provider4)
		
		when(provider1.getRolesFor(currentUser)) thenReturn(Seq(Sysadmin()))
		when(provider2.getRolesFor(currentUser, module)) thenReturn(Seq(ModuleManager(module)))
		when(provider3.getRolesFor(currentUser, dept)) thenReturn(Seq(DepartmentalAdministrator(dept)))
		when(provider3.getRolesFor(currentUser, module)) thenReturn(Seq())
		when(provider4.getRolesFor(currentUser)) thenReturn(Seq())
		
		(service.getRolesFor(currentUser, module) exists { _ == DepartmentalAdministrator(dept) }) should be (true)
		
		there was one(provider1).getRolesFor(currentUser)
		there was one(provider4).getRolesFor(currentUser)
		
		there was no(provider2).getRolesFor(currentUser, dept) // We don't bubble up on this one because it's not exhaustive
		there was one(provider2).getRolesFor(currentUser, module)
		there was one(provider3).getRolesFor(currentUser, dept)
		there was one(provider3).getRolesFor(currentUser, module)
	}
	
	@Test def exhaustiveGetRoles() = withUser("cuscav", "0672089") {
		val provider = mock[RoleProvider]
		
		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.department = dept
		
		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider)
		
		when(provider.getRolesFor(currentUser, module)) thenReturn(Seq(ModuleManager(module)))
		when(provider.getRolesFor(currentUser, dept)) thenReturn(Seq(DepartmentalAdministrator(dept)))
		
		(service.getRolesFor(currentUser, module) exists { _ == DepartmentalAdministrator(dept) }) should be (false)
		
		provider.isExhaustive returns (true)
		
		(service.getRolesFor(currentUser, module) exists { _ == DepartmentalAdministrator(dept) }) should be (true)
	}
	
	@Test def hasRole() = withUser("cuscav", "0672089") {
		val provider1 = mock[ScopelessRoleProvider]
		provider1.rolesProvided returns (Set(classOf[Sysadmin]))
		
		val provider2 = mock[RoleProvider]
		provider2.rolesProvided returns (Set(classOf[ModuleManager]))
		
		val provider3 = mock[RoleProvider]
		provider3.rolesProvided returns (Set(classOf[DepartmentalAdministrator]))
		
		val provider4 = mock[ScopelessRoleProvider]
		provider4.rolesProvided returns (Set())
		
		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		
		val service = new RoleServiceImpl()
		service.roleProviders = Array(provider1, provider2, provider3, provider4)
		
		when(provider2.getRolesFor(currentUser, module)) thenReturn(Seq(ModuleManager(module)))
		
		service.hasRole(currentUser, ModuleManager(module)) should be (true)
		service.hasRole(currentUser, ExtensionManager(dept)) should be (false)
	}
	
	@Test def getPermissions() = withUser("cuscav", "0672089") {
		val provider1 = mock[PermissionsProvider]
		val provider2 = mock[PermissionsProvider]
		
		val dept = Fixtures.department("in")
		val module = Fixtures.module("in101")
		module.department = dept
		
		val service = new RoleServiceImpl()
		service.permissionsProviders = Array(provider1, provider2)
		
		when(provider1.getPermissionsFor(currentUser, module)) thenReturn(Stream(PermissionDefinition(Permissions.Module.Read, Some(module), GrantedPermission.Allow)))
		when(provider2.getPermissionsFor(currentUser, dept)) thenReturn(Stream(PermissionDefinition(Permissions.Module.Create, Some(dept), GrantedPermission.Allow)))
		when(provider2.getPermissionsFor(currentUser, module)) thenReturn(Stream())
		
		(service.getExplicitPermissionsFor(currentUser, module) exists { 
			_ == PermissionDefinition(Permissions.Module.Create, Some(dept), GrantedPermission.Allow) 
		}) should be (true)
				
		there was no(provider1).getPermissionsFor(currentUser, dept) // We don't bubble up on this one because it's not exhaustive
		there was one(provider1).getPermissionsFor(currentUser, module)
		there was one(provider2).getPermissionsFor(currentUser, dept)
		there was one(provider2).getPermissionsFor(currentUser, module)
	}

}