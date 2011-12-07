package uk.ac.warwick.courses.services

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.DepartmentDao
import uk.ac.warwick.courses.data.ModuleDao
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.userlookup.GroupService
import org.springframework.scheduling.annotation.Async
import collection.JavaConverters._
import uk.ac.warwick.userlookup.Group
import uk.ac.warwick.courses.commands.imports.ImportModulesCommand

/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging  {
  
    @Autowired var moduleDao:ModuleDao =_
    @Autowired var departmentDao:DepartmentDao =_
    @Autowired var groupService:GroupService =_

    
    @Transactional(readOnly=true)
    def allDepartments = departmentDao.allDepartments
    
    @Transactional(readOnly=true)
    def getDepartmentByCode(code:String) = departmentDao.getByCode(code)
    
    @Transactional(readOnly=true)
    def getModuleByCode(code:String) = moduleDao.getByCode(code)
   
    
    def departmentsOwnedBy(usercode:String) = departmentDao.getByOwner(usercode)
    
    /**
     * Modules that this user is attending.
     * TODO assumes webgroup = module attendance.
     */
    def modulesAttendedBy(usercode:String) = groupService.getGroupsForUser(usercode).asScala
		.filter { "Module" equals _.getType }
		
    
    
    @Transactional
    def addOwner(dept:Department, owner:String) = dept.owners.addUser(owner)
    
    @Transactional
    def removeOwner(dept:Department, owner:String) = dept.owners.removeUser(owner)
    
    

}