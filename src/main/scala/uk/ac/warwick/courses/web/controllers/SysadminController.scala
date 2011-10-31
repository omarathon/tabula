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
import uk.ac.warwick.courses.web.forms.DepartmentAddOwnerForm
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.courses.web.forms.DepartmentRemoveOwnerForm
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.data.model.Department

package sysadmin {

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 */
  
@Controller
@RequestMapping(Array("/sysadmin"))
class SysadminController extends Logging {

	@Autowired var moduleService:ModuleAndDepartmentService = null
  
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
	
	// Department owners
	
	@RequestMapping(value=Array("/departments/{dept}/owners"), method=Array(RequestMethod.GET))
	def viewDepartmentOwners(@PathVariable dept:Department) = {
		Mav("sysadmin/departments/owners",
			  		  "department" -> dept,
			  		  "owners" -> dept.owners)
	}
	
	@ModelAttribute("addOwner") def addOwnerForm(@PathVariable("dept") dept:Department) = {
		new DepartmentAddOwnerForm(dept.owners.members)
	}
	
	@RequestMapping(value=Array("/departments/{dept}/owners/add"), method=Array(RequestMethod.GET))
	def addDeptOwnerForm(@PathVariable dept:Department, @ModelAttribute("addOwner") form:DepartmentAddOwnerForm, errors:Errors):ModelAndView = {
		new ModelAndView("sysadmin/departments/owners/add")
			.addObject("department", dept)
	}
	
	@RequestMapping(value=Array("/departments/{dept}/owners/add"), method=Array(RequestMethod.POST))
	def addDeptOwner(@PathVariable dept:Department, @Valid @ModelAttribute("addOwner") form:DepartmentAddOwnerForm, errors:Errors):ModelAndView  = {
		if (errors.hasErrors) {
		  return addDeptOwnerForm(dept, form, errors)
		} else {
		  logger.info("Passed validation, saving owner")
		  moduleService.addOwner(dept, form.getUsercode())
		  return redirectToDeptOwners(dept.code)
		}
	}
	
	@RequestMapping(value=Array("/departments/{dept}/owners/delete"), method=Array(RequestMethod.POST))
	def addDeptOwner(@PathVariable dept:Department, @Valid @ModelAttribute("removeOwner") form:DepartmentRemoveOwnerForm, errors:Errors):ModelAndView  = {
		if (errors.hasErrors) {
		  return viewDepartmentOwners(dept)
		} else {
		  logger.info("Passed validation, saving owner")
		  moduleService.removeOwner(dept, form.getUsercode())
		  return redirectToDeptOwners(dept.code)
		}
	}
	
	def redirectToDeptOwners(deptcode:String) = new ModelAndView("redirect:/sysadmin/departments/"+deptcode+"/owners/")
	
	// End department owners
	
	@RequestMapping(value=Array("/import"), method=Array(RequestMethod.POST))
	def importModules = {
		  moduleService.importData
		  "sysadmin/importdone"
	}

	
}


}
