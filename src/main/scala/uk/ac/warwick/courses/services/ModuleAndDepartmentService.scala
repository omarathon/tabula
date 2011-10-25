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


/**
 * Handles data about modules and departments
 */
@Service
class ModuleAndDepartmentService extends Logging  {
  
    @Autowired var moduleImporter:ModuleImporter =_
    @Autowired var moduleDao:ModuleDao =_
    @Autowired var departmentDao:DepartmentDao =_
    @Autowired var groupService:GroupService =_
  
    @Scheduled(cron="0 0 7,14 * * *")
    @Transactional
    def importData {
      importDepartments
      importModules
    }
    
    @Transactional(readOnly=true)
    def allDepartments = departmentDao.allDepartments
    
    @Transactional(readOnly=true)
    def getDepartmentByCode(code:String) = departmentDao.getByCode(code)
    
    @Transactional(readOnly=true)
    def getModuleByCode(code:String) = moduleDao.getByCode(code)
    
    def importModules {
      logger.info("Importing modules")
      for (dept <- allDepartments) {
        for (mod <- moduleImporter.getModules(dept.code)) {
          moduleDao.getByCode(mod.code) match {
            case None => {
              logger.debug("Mod code " + mod.code + " not found in database, so inserting")
              moduleDao.saveOrUpdate(newModuleFrom(mod, dept))
            }
            case Some(module) => { }
          }
        }
      }
    }
    
    @Transactional
    def addOwner(dept:Department, owner:String) = dept.owners.addUser(owner)
    
    @Transactional
    def removeOwner(dept:Department, owner:String) = dept.owners.removeUser(owner)
    
    def importDepartments {
      // TODO throttle?
      logger.info("Importing departments")
      for (dept <- moduleImporter.getDepartments) {
        dept.faculty match {
          case "Service/Admin" => logger.debug("Skipping Service/Admin department " + dept.code)
          case _ => {
            departmentDao.getByCode(dept.code) match {
              case None => departmentDao save newDepartmentFrom(dept)
              case Some(dept) => { logger.debug("Skipping " + dept.code + " as it is already in the database") }
            }
          }
        }
      }
    }
    
    private def newModuleFrom(m:ModuleInfo, dept:Department): Module = {
      val module = new Module
      module.code = m.code
      module.name = m.name
      module.webgroup = m.group
      module.department = dept
      module
    }
    
    private def newDepartmentFrom(d:DepartmentInfo): Department = {
      val department = new Department
      department.code = d.code
      department.name = d.name
      department
    }

}