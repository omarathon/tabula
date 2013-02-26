package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedRole
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.data.model.permissions.RoleOverride
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.permissions.DepartmentGrantedPermission
import uk.ac.warwick.tabula.data.model.permissions.GrantedPermission

class PermissionsServiceTest extends AppContextTestBase {
	
	@Autowired var service: PermissionsService = _
	
	@Test def crud = transactional { t =>
		val dept1 = Fixtures.department("dp1")
		val dept2 = Fixtures.department("dp2")
		
		session.save(dept1)
		session.save(dept2)
		session.flush()
	
		val gr1 = new DepartmentGrantedRole(dept1, DepartmentalAdministratorRoleDefinition)
		gr1.users.addUser("cuscav")
		gr1.users.addUser("cusebr")
		service.saveOrUpdate(gr1)
		
		val crd = new CustomRoleDefinition
		crd.department = dept1
		crd.name = "Custom def"
		crd.builtInBaseRoleDefinition = ModuleManagerRoleDefinition
		
		val ro = new RoleOverride
		ro.permission = Permissions.Module.Read
		ro.overrideType = ro.Deny
		
		crd.overrides.add(ro)
		
		service.saveOrUpdate(crd)
		
		val gr2 = new DepartmentGrantedRole(dept1, crd)
		gr2.users.addUser("cuscav")
		gr2.users.addUser("cuscao")
		service.saveOrUpdate(gr2)
		
		val gp = new DepartmentGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Allow)
		gp.users.addUser("cuscav")
		gp.users.addUser("cuscao")
		service.saveOrUpdate(gp)
		
		session.flush()
		
		service.getGrantedRole(dept1, DepartmentalAdministratorRoleDefinition) should be (Some(gr1))
		service.getGrantedRole(dept1, crd) should be (Some(gr2))
		service.getGrantedRole(dept1, ModuleManagerRoleDefinition) should be (None)
		service.getGrantedRole(dept2, DepartmentalAdministratorRoleDefinition) should be (None)
		service.getGrantedRole(dept2, crd) should be (None)
		
		service.getGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Allow) should be (Some(gp))
		service.getGrantedPermission(dept1, Permissions.Module.Create, GrantedPermission.Deny) should be (None)
		service.getGrantedPermission(dept1, Permissions.Module.Read, GrantedPermission.Allow) should be (None)
		service.getGrantedPermission(dept2, Permissions.Module.Create, GrantedPermission.Allow) should be (None)
		
		withUser("cuscav") {
			service.getGrantedRolesFor(currentUser, dept1).toSet should be (Set(gr1, gr2))
			service.getGrantedRolesFor(currentUser, dept2) should be (Seq())
			
			service.getGrantedPermissionsFor(currentUser, dept1) should be (Seq(gp))
			service.getGrantedPermissionsFor(currentUser, dept2) should be (Seq())
		}
		
		withUser("cuscao") {
			service.getGrantedRolesFor(currentUser, dept1) should be (Seq(gr2))
			service.getGrantedRolesFor(currentUser, dept2) should be (Seq())
			
			service.getGrantedPermissionsFor(currentUser, dept1) should be (Seq(gp))
			service.getGrantedPermissionsFor(currentUser, dept2) should be (Seq())
		}
		
		withUser("curef") {
			service.getGrantedRolesFor(currentUser, dept1) should be (Seq())
			service.getGrantedRolesFor(currentUser, dept2) should be (Seq())
			
			service.getGrantedPermissionsFor(currentUser, dept1) should be (Seq())
			service.getGrantedPermissionsFor(currentUser, dept2) should be (Seq())
		}
		
		service.ensureUserGroupFor(dept1, DepartmentalAdministratorRoleDefinition) should be (gr1.users)
		service.ensureUserGroupFor(dept2, DepartmentalAdministratorRoleDefinition).id should not be (null)
	}

}