package uk.ac.warwick.tabula.services
import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.DepartmentDao
import uk.ac.warwick.tabula.data.ModuleDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.permissions.GrantedRole
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.roles.RouteManagerRoleDefinition
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.RouteDao

/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging {

	@Autowired var moduleDao: ModuleDao = _
	@Autowired var routeDao: RouteDao = _
	@Autowired var departmentDao: DepartmentDao = _
	@Autowired var userLookup: UserLookupService = _
	@Autowired var securityService: SecurityService = _
	@Autowired var permissionsService: PermissionsService = _
	def groupService = userLookup.getGroupService

	def allDepartments = transactional(readOnly = true) {
		departmentDao.allDepartments
	}

	def allModules = transactional(readOnly = true) {
		moduleDao.allModules
	}

	def allRoutes = transactional(readOnly = true) {
		routeDao.allRoutes
	}

	def getDepartmentByCode(code: String) = transactional(readOnly = true) {
		departmentDao.getByCode(code)
	}

	def getDepartmentById(code: String) = transactional(readOnly = true) {
		departmentDao.getById(code)
	}

	def getModuleByCode(code: String) = transactional(readOnly = true) {
		moduleDao.getByCode(code)
	}

	def getModuleBySitsCode(sitsModuleCode: String) = transactional(readOnly = true) {
		moduleDao.getByCode(Module.stripCats(sitsModuleCode).toLowerCase())
	}

	def getModuleById(code: String) = transactional(readOnly = true) {
		moduleDao.getById(code)
	}

	def getRouteByCode(code: String) = transactional(readOnly = true) {
		routeDao.getByCode(code)
	}

	def getRouteById(id: String) = transactional(readOnly = true) {
		routeDao.getById(id)
	}

	// We may have a granted role that's overridden later, so we also need to do a security service check as well
	// as getting the role itself

	def departmentsWithPermission(user: CurrentUser, permission: Permission): Set[Department] =
		permissionsService.getAllPermissionDefinitionsFor[Department](user, permission)
			.filter { department => securityService.can(user, permission, department) }

	def modulesWithPermission(user: CurrentUser, permission: Permission): Set[Module] =
		permissionsService.getAllPermissionDefinitionsFor[Module](user, permission)
			.filter { module => securityService.can(user, permission, module) }

	def modulesWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Module] =
		modulesWithPermission(user, permission).filter { _.department == dept }

	def modulesInDepartmentsWithPermission(user: CurrentUser, permission: Permission) = {
		departmentsWithPermission(user, permission) flatMap (dept => dept.modules.asScala)
	}
	def modulesInDepartmentWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Module] = {
		if (departmentsWithPermission(user, permission) contains dept) dept.modules.asScala.toSet else Set()
	}
	
	def routesWithPermission(user: CurrentUser, permission: Permission): Set[Route] =
		permissionsService.getAllPermissionDefinitionsFor[Route](user, permission)
			.filter { route => securityService.can(user, permission, route) }

	def routesWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Route] =
		routesWithPermission(user, permission).filter { _.department == dept }

	def routesInDepartmentsWithPermission(user: CurrentUser, permission: Permission) = {
		departmentsWithPermission(user, permission) flatMap (dept => dept.routes.asScala)
	}
	def routesInDepartmentWithPermission(user: CurrentUser, permission: Permission, dept: Department): Set[Route] = {
		if (departmentsWithPermission(user, permission) contains dept) dept.routes.asScala.toSet else Set()
	}

	private def getRole[A <: PermissionsTarget : ClassTag](target: A, defn: RoleDefinition) =
		permissionsService.getGrantedRole(target, defn) match {
			case Some(role) => role
			case _ => GrantedRole(target, defn)
		}

	def addOwner(dept: Department, owner: String) = transactional() {
		val role = getRole(dept, DepartmentalAdministratorRoleDefinition)
		role.users.addUser(owner)
		permissionsService.saveOrUpdate(role)
	}

	def removeOwner(dept: Department, owner: String) = transactional() {
		val role = getRole(dept, DepartmentalAdministratorRoleDefinition)
		role.users.removeUser(owner)
		permissionsService.saveOrUpdate(role)
	}

	def addModuleManager(module: Module, owner: String) = transactional() {
		val role = getRole(module, ModuleManagerRoleDefinition)
		role.users.addUser(owner)
		permissionsService.saveOrUpdate(role)
	}

	def removeModuleManager(module: Module, owner: String) = transactional() {
		val role = getRole(module, ModuleManagerRoleDefinition)
		role.users.removeUser(owner)
		permissionsService.saveOrUpdate(role)
	}

	def addRouteManager(route: Route, owner: String) = transactional() {
		val role = getRole(route, RouteManagerRoleDefinition)
		role.users.addUser(owner)
		permissionsService.saveOrUpdate(role)
	}

	def removeRouteManager(route: Route, owner: String) = transactional() {
		val role = getRole(route, RouteManagerRoleDefinition)
		role.users.removeUser(owner)
		permissionsService.saveOrUpdate(role)
	}

	def save(dept: Department) = transactional() {
		departmentDao.save(dept)
	}

}

trait ModuleAndDepartmentServiceComponent {
	def moduleAndDepartmentService: ModuleAndDepartmentService
}

trait AutowiringModuleAndDepartmentServiceComponent extends ModuleAndDepartmentServiceComponent {
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

}