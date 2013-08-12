package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula._
import org.junit.Before
import uk.ac.warwick.tabula.permissions.{Permission, PermissionsTarget, Permissions}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.services.permissions._
import scala.Some
import uk.ac.warwick.util.queue.Queue

class ModuleAndDepartmentServiceTest extends PersistenceTestBase with Mockito {
	
	val service: ModuleAndDepartmentService = new ModuleAndDepartmentService

	val userLookup = new MockUserLookup
	
	@Before def wire {
		val departmentDao = new DepartmentDaoImpl
		departmentDao.sessionFactory = sessionFactory
		service.departmentDao = departmentDao

		service.userLookup = userLookup

		val moduleDao = new ModuleDaoImpl
		moduleDao.sessionFactory = sessionFactory
		service.moduleDao = moduleDao

		val permsDao = new PermissionsDaoImpl
		permsDao.sessionFactory = sessionFactory

		val permissionsService = new PermissionsServiceImpl with PermissionsDaoComponent with PermissionsServiceCaches {
			var permissionsDao:PermissionsDao = permsDao
			val rolesByIdCache:GrantedRoleByIdCache = new GrantedRoleByIdCache(permsDao)
			val permissionsByIdCache = new GrantedPermissionsByIdCache(permsDao)
		}
		permissionsService.queue = mock[Queue]
		permissionsService.groupService = userLookup.getGroupService()
		service.permissionsService = permissionsService

		val securityService = mock[SecurityService]
		securityService.can(isA[CurrentUser],isA[Permission],isA[PermissionsTarget] ) returns true
		service.securityService = securityService


	}
	
	@Test def crud = transactional { tx =>
		// uses data created in data.sql
		
		val ch = service.getDepartmentByCode("ch").get
		val cs = service.getDepartmentByCode("cs").get
		val cssub = service.getDepartmentByCode("cs-subsidiary").get
		
		val cs108 = service.getModuleByCode("cs108").get
		val cs240 = service.getModuleByCode("cs240").get
		val cs241 = service.getModuleByCode("cs241").get
		
		service.allDepartments should be (Seq(ch, cs, cssub))
		service.allModules should be (Seq(cs108, cs240, cs241))
		
		// behaviour of child/parent departments
		cs.children.toArray should be (Array(cssub))
		cssub.parent should be (cs)
		ch.children.isEmpty should be (true)
		cs241.department should be (cssub)
		
		service.getDepartmentByCode("ch") should be (Some(ch))
		service.getDepartmentById(ch.id) should be (Some(ch))
		service.getDepartmentByCode("wibble") should be (None)
		service.getDepartmentById("wibble") should be (None)
		
		service.getModuleByCode("cs108") should be (Some(cs108))
		service.getModuleById(cs108.id) should be (Some(cs108))
		service.getModuleByCode("wibble") should be (None)
		service.getModuleById("wibble") should be (None)
		
		val route = Fixtures.route("g503", "MEng Computer Science")
		session.save(route)
		
		//service.getRouteByCode("g503") should be (Some(route))
		//service.getRouteByCode("wibble") should be (None)
		
		withUser("cusebr") { service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs)) }
		withUser("cuscav") { 
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesinDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set())
			service.modulesinDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			
			service.addOwner(cs, "cuscav")
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs))
			service.modulesInDepartmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108, cs240))
			service.modulesinDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108, cs240))
			service.modulesinDepartmentWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			
			service.removeOwner(cs, "cuscav")
			service.departmentsWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set())
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set())
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
			
			service.addManager(cs108, "cuscav")
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, cs) should be (Set(cs108))
			service.modulesWithPermission(currentUser, Permissions.Module.ManageAssignments, ch) should be (Set())
		}
	}

}
