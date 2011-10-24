package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.services.ModuleService
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

package sysadmin {

@Controller
@RequestMapping(Array("/sysadmin"))
class SysadminController extends Logging {

	@Autowired var moduleService:ModuleService = null
  
	@RequestMapping(Array("/home"))
	def home = "sysadmin/home"
	  
	@RequestMapping(Array("/departments/"))
	def departments = new ModelAndView("sysadmin/departments/list")
		.addObject("departments", moduleService.allDepartments)
	
	@RequestMapping(Array("/departments/{deptcode}/"))
	def department(@PathVariable deptcode:String) = {
		moduleService.getDepartmentByCode(deptcode) match {
		  case Some(dept) => {
			  	new ModelAndView("sysadmin/departments/single")
			  		.addObject("department", dept)
		  }
		  case None => throw new IllegalArgumentException
		}
	}
	
	// Department owners
	
	@RequestMapping(value=Array("/departments/{deptcode}/owners"), method=Array(RequestMethod.GET))
	def viewDepartmentOwners(@PathVariable deptcode:String) = {
		moduleService.getDepartmentByCode(deptcode) match {
		  case Some(dept) => {
			  	new ModelAndView("sysadmin/departments/owners")
			  		.addObject("department", dept)
			  		.addObject("owners", dept.owners)
		  }
		  case None => throw new IllegalArgumentException
		}
	}
	
	@ModelAttribute("addOwner") def addOwnerForm(@PathVariable("deptcode")deptcode:String) = {
	  val dept = moduleService.getDepartmentByCode(deptcode).get
	  new DepartmentAddOwnerForm(dept.owners.members)
	}
	
	@RequestMapping(value=Array("/departments/{deptcode}/owners/add"), method=Array(RequestMethod.GET))
	def addDeptOwnerForm(@PathVariable deptcode:String, @ModelAttribute("addOwner") form:DepartmentAddOwnerForm, errors:Errors):ModelAndView = {
		val dept = moduleService.getDepartmentByCode(deptcode).get
		new ModelAndView("sysadmin/departments/owners/add")
			.addObject("department", dept)
	}
	
	@RequestMapping(value=Array("/departments/{deptcode}/owners/add"), method=Array(RequestMethod.POST))
	def addDeptOwner(@PathVariable deptcode:String, @Valid @ModelAttribute("addOwner") form:DepartmentAddOwnerForm, errors:Errors):ModelAndView  = {
		if (errors.hasErrors) {
		  return addDeptOwnerForm(deptcode, form, errors)
		} else {
		  logger.info("Passed validation, saving owner")
		  val dept = moduleService.getDepartmentByCode(deptcode).get
		  moduleService.addOwner(dept, form.getUsercode())
		  return redirectToDeptOwners(dept.code)
		}
	}
	
	@RequestMapping(value=Array("/departments/{deptcode}/owners/delete"), method=Array(RequestMethod.POST))
	def addDeptOwner(@PathVariable deptcode:String, @Valid @ModelAttribute("removeOwner") form:DepartmentRemoveOwnerForm, errors:Errors):ModelAndView  = {
		if (errors.hasErrors) {
		  return viewDepartmentOwners(deptcode)
		} else {
		  logger.info("Passed validation, saving owner")
		  val dept = moduleService.getDepartmentByCode(deptcode).get
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
