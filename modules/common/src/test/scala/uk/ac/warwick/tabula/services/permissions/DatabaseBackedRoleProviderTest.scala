package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedRole
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.DepartmentModuleManager
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedPermission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission

class DatabaseBackedRoleProviderTest extends TestBase with Mockito {
	
	val provider = new DatabaseBackedRoleProvider
	
	val service = mock[PermissionsService]
	provider.service = service
	
	val dept = Fixtures.department("in")
	
	@Test def getRoles = withUser("cuscav") {
		val gr1 = new DepartmentGrantedRole(dept, DepartmentalAdministratorRoleDefinition)
		val gr2 = new DepartmentGrantedRole(dept, ModuleManagerRoleDefinition)
		
		service.getGrantedRolesFor(currentUser, dept) returns (Seq(gr1, gr2))
		
		// Can't do exact equality because a granted role with no overrides is still a generated role, not a strict built in role
		val roles = provider.getRolesFor(currentUser, dept)
		roles.size should be (2)
		roles(0).explicitPermissions should be (DepartmentalAdministrator(dept).explicitPermissions)
		roles(1).explicitPermissions should be (DepartmentModuleManager(dept).explicitPermissions)
	}
	
	@Test def noRoles = withUser("cuscav") {
		service.getGrantedRolesFor(currentUser, dept) returns (Seq())
		provider.getRolesFor(currentUser, dept) should be (Seq())
	}
	
	@Test def getPermissions = withUser("cuscav") {
		val gp1 = new DepartmentGrantedPermission(dept, Permissions.Department.ManageDisplaySettings, GrantedPermission.Allow)
		val gp2 = new DepartmentGrantedPermission(dept, Permissions.Module.Create, GrantedPermission.Deny)
		
		service.getGrantedPermissionsFor(currentUser, dept) returns (Seq(gp1, gp2))
		
		val permissions = provider.getPermissionsFor(currentUser, dept)
		permissions.size should be (2)
		permissions(0).permission should be (Permissions.Department.ManageDisplaySettings)
		permissions(0).scope should be (Some(dept))
		permissions(0).permissionType should be (GrantedPermission.Allow)
		permissions(1).permission should be (Permissions.Module.Create)
		permissions(1).scope should be (Some(dept))
		permissions(1).permissionType should be (GrantedPermission.Deny)
	}
	
	@Test def noPermissions = withUser("cuscav") {
		service.getGrantedPermissionsFor(currentUser, dept) returns (Seq())
		provider.getPermissionsFor(currentUser, dept) should be (Seq())
	}

}