package uk.ac.warwick.courses.commands.imports

import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.commands._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional


@Configurable
class ImportModulesCommand extends Command[Unit] with Logging with Daoisms {

	@Autowired var moduleImporter:ModuleImporter =_
	@Autowired var moduleService:ModuleAndDepartmentService =_
	
	@Transactional
	def apply() { 
		importDepartments
		importModules
	}

	def describe(d: Description) {
		
	}
	
	def importModules {
    	logger.info("Importing modules")
    	for (dept <- moduleService.allDepartments) {
    		for (mod <- moduleImporter.getModules(dept.code)) {
    			moduleService.getModuleByCode(mod.code) match {
	    			case None => {
	    				debug("Mod code %s not found in database, so inserting", mod.code)
	    				session.saveOrUpdate(newModuleFrom(mod, dept))
	    			}
	    			case Some(module) => { }
    			}
    		}
    	}
    }
	
    def importDepartments {
      logger.info("Importing departments")
      for (dept <- moduleImporter.getDepartments) {
        moduleService.getDepartmentByCode(dept.code) match {
          case None => session.save(newDepartmentFrom(dept))
          case Some(dept) => { debug("Skipping %s as it is already in the database", dept.code) }
        }
      }
    }
    
    private def newModuleFrom(m:ModuleInfo, dept:Department): Module = {
      val module = new Module
      module.code = m.code
      module.name = m.name
//      module.members.baseWebgroup = m.group
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