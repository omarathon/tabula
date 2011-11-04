package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.bind.annotation.PathVariable
import org.hibernate.Hibernate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.commands.departments.RemoveDeptOwnerCommand
import uk.ac.warwick.courses.commands.departments.AddDeptOwnerCommand

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 */

class BaseSysadminController extends Controllerism {
	@Autowired var moduleService:ModuleAndDepartmentService = null
	
	def redirectToDeptOwners(deptcode:String) = new ModelAndView("redirect:/sysadmin/departments/"+deptcode+"/owners/")
	
	@RequestMapping(value=Array("/sysadmin/departments/{dept}/owners"), method=Array(RequestMethod.GET))
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
		def addDeptOwner(@PathVariable dept:Department, @Valid @ModelAttribute("removeOwner") form:RemoveDeptOwnerCommand, errors:Errors):ModelAndView  = {
			if (errors.hasErrors) {
			  return viewDepartmentOwners(dept)
			} else {
			  logger.info("Passed validation, removing owner")
			  form.apply()
			  return redirectToDeptOwners(dept.code)
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
		def addDeptOwnerForm(@PathVariable dept:Department, @ModelAttribute("addOwner") form:AddDeptOwnerCommand, errors:Errors):ModelAndView = {
			Mav("sysadmin/departments/owners/add",
				"department" -> dept)
		}
		
		@RequestMapping(method=Array(RequestMethod.POST))
		def addDeptOwner(@PathVariable dept:Department, @Valid @ModelAttribute("addOwner") form:AddDeptOwnerCommand, errors:Errors):ModelAndView  = {
			if (errors.hasErrors) {
			  return addDeptOwnerForm(dept, form, errors)
			} else {
			  logger.info("Passed validation, saving owner")
			  form.apply()
			  return redirectToDeptOwners(dept.code)
			}
		}
	}
	
}


