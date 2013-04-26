package uk.ac.warwick.tabula.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.DepartmentDao
import uk.ac.warwick.tabula.data.ModuleDao
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.GroupService
import org.springframework.scheduling.annotation.Async
import collection.JavaConverters._
import uk.ac.warwick.userlookup.Group
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.RouteDao
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions

/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging {

	@Autowired var moduleDao: ModuleDao = _
	@Autowired var departmentDao: DepartmentDao = _
	@Autowired var routeDao: RouteDao = _
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

	def getDepartmentByCode(code: String) = transactional(readOnly = true) {
		departmentDao.getByCode(code)
	}

	def getDepartmentById(code: String) = transactional(readOnly = true) {
		departmentDao.getById(code)
	}

	def getModuleByCode(code: String) = transactional(readOnly = true) {
		moduleDao.getByCode(code)
	}

	def getModuleById(code: String) = transactional(readOnly = true) {
		moduleDao.getById(code)
	}
	
	def getRouteByCode(code: String) = transactional(readOnly = true) {
		routeDao.getByCode(code)
	} 

	// We may have a granted role that's overridden later, so we also need to do a security service check as well
	// as getting the role itself
	
	def departmentsOwnedBy(user: CurrentUser): Set[Department] = 
		permissionsService.getAllPermissionDefinitionsFor[Department](user, Permissions.Module.Read)
			.filter { department => securityService.can(user, Permissions.Module.Read, department) }

	def modulesManagedBy(user: CurrentUser): Set[Module] = 
		permissionsService.getAllPermissionDefinitionsFor[Module](user, Permissions.Module.Read)
			.filter { module => securityService.can(user, Permissions.Module.Read, module) }
	
	def modulesManagedBy(user: CurrentUser, dept: Department): Set[Module] = 
		modulesManagedBy(user).filter { _.department == dept }
	
	def modulesAdministratedBy(user: CurrentUser) = {
		departmentsOwnedBy(user) flatMap (dept => dept.modules.asScala)
	}
	def modulesAdministratedBy(user: CurrentUser, dept: Department) = {
		if (departmentsOwnedBy(user) contains dept) dept.modules.asScala else Nil
	}

	def addOwner(dept: Department, owner: String) = transactional() {
		dept.owners.addUser(owner)
	}

	def removeOwner(dept: Department, owner: String) = transactional() {
		dept.owners.removeUser(owner)
	}
	
	def save(dept: Department) = transactional() {
		departmentDao.save(dept)
	}

}