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

/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging {

	@Autowired var moduleDao: ModuleDao = _
	@Autowired var departmentDao: DepartmentDao = _
	@Autowired var routeDao: RouteDao = _
	@Autowired var userLookup: UserLookupService = _
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

	def departmentsOwnedBy(usercode: String) = departmentDao.getByOwner(usercode)

	def modulesManagedBy(usercode: String) = moduleDao.findByParticipant(usercode)
	def modulesManagedBy(usercode: String, dept: Department) = moduleDao.findByParticipant(usercode, dept)
	
	def modulesAdministratedBy(usercode: String) = {
		departmentsOwnedBy(usercode).toSeq flatMap (dept => dept.modules.asScala)
	}
	def modulesAdministratedBy(usercode: String, dept: Department) = {
		if (departmentsOwnedBy(usercode) contains dept) dept.modules.asScala else Nil
	}

	def addOwner(dept: Department, owner: String) = transactional() {
		dept.owners.addUser(owner)
	}

	def removeOwner(dept: Department, owner: String) = transactional() {
		dept.owners.removeUser(owner)
	}

}