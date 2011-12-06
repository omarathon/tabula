package uk.ac.warwick.courses.web.controllers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import javax.validation.Valid
import uk.ac.warwick.courses.commands.departments.AddDeptOwnerCommand
import uk.ac.warwick.courses.commands.departments.RemoveDeptOwnerCommand
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.validators.UniqueUsercode
import uk.ac.warwick.courses.web.controllers.Controllerism

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 */

class BaseSysadminController extends Controllerism {
	@Autowired var moduleService:ModuleAndDepartmentService = null
	
	def redirectToDeptOwners(deptcode:String) = Mav("redirect:/sysadmin/departments/"+deptcode+"/owners/")
	
	
	def viewDepartmentOwners(@PathVariable dept:Department) = 
		Mav("sysadmin/departments/owners",
			  		  "department" -> dept,
			  		  "owners" -> dept.owners)
	
}
	
@Controller
@RequestMapping(Array("/sysadmin"))
class SysadminController extends BaseSysadminController {
  
	@RequestMapping(Array("/home"))
	def home = "sysadmin/home"
		
	@RequestMapping(value=Array("/departments/{dept}/owners/"), method=Array(RequestMethod.GET))
	def departmentOwners(@PathVariable dept:Department) = viewDepartmentOwners(dept)
	  
	@RequestMapping(Array("/departments/"))
	def departments = Mav("sysadmin/departments/list",
			"departments" -> moduleService.allDepartments
	)
	
	@RequestMapping(Array("/departments/{dept}/"))
	def department(@PathVariable dept:Department) = {
  		Mav("sysadmin/departments/single",
  					  "department" -> dept)
	}
	
	@RequestMapping(value=Array("/import"), method=Array(RequestMethod.POST))
	def importModules = {
		  moduleService.importData
		  "sysadmin/importdone"
	}

}

object SysadminController {
	
	@Controller 
	@RequestMapping(Array("/sysadmin/departments/{dept}/owners/delete"))
	class RemoveDeptOwnerController extends BaseSysadminController {
		@ModelAttribute("removeOwner") def addOwnerForm(@PathVariable("dept") dept:Department) = {
			new RemoveDeptOwnerCommand(dept)
		}
		
		@RequestMapping(method=Array(RequestMethod.POST))
		def addDeptOwner(@PathVariable dept:Department, @Valid @ModelAttribute("removeOwner") form:RemoveDeptOwnerCommand, errors:Errors)  = {
			if (errors.hasErrors) {
			  viewDepartmentOwners(dept)
			} else {
			  logger.info("Passed validation, removing owner")
			  form.apply()
			  redirectToDeptOwners(dept.code)
			}
		}
	}
	
	
	@Controller 
	@RequestMapping(Array("/sysadmin/departments/{dept}/owners/add"))
	class AddDeptOwnerController extends BaseSysadminController {
	
		@ModelAttribute("addOwner") def addOwnerForm(@PathVariable("dept") dept:Department) = {
			new AddDeptOwnerCommand(dept)
		}
		
		@RequestMapping(method=Array(RequestMethod.GET))
		def showForm(@PathVariable dept:Department, @ModelAttribute("addOwner") form:AddDeptOwnerCommand, errors:Errors) = {
			Mav("sysadmin/departments/owners/add",
				"department" -> dept)
		}
		
		@RequestMapping(method=Array(RequestMethod.POST))
		def submit(@PathVariable dept:Department, @Valid @ModelAttribute("addOwner") form:AddDeptOwnerCommand, errors:Errors)  = {
			if (errors.hasErrors) {
			  showForm(dept, form, errors)
			} else {
			  logger.info("Passed validation, saving owner")
			  form.apply()
			  redirectToDeptOwners(dept.code)
			}
		}
	}
	
	@Controller
	@RequestMapping(Array("/sysadmin/masquerade"))
	class MasqueradeController extends BaseSysadminController {
		
	}
	
}


