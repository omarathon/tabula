package uk.ac.warwick.courses.web.controllers
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.services.ModuleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.bind.annotation.PathVariable
import org.hibernate.Hibernate

@Controller
class SysadminController {

	@Autowired var moduleService:ModuleService = null
  
	@RequestMapping(Array("/sysadmin/home"))
	def home = "sysadmin/home"
	  
	@RequestMapping(Array("/sysadmin/departments"))
	def departments = new ModelAndView("sysadmin/departments")
		.addObject("departments", moduleService.allDepartments)
	
	@RequestMapping(Array("/sysadmin/departments/{deptcode}"))
	def department(@PathVariable deptcode:String) = {
		moduleService.getDepartmentByCode(deptcode) match {
		  case Some(dept) => {
			  	new ModelAndView("sysadmin/department")
			  		.addObject("department", dept)
		  }
		  case None => throw new IllegalArgumentException
		}
	}
	  
	@RequestMapping(value=Array("/sysadmin/import"), method=Array(RequestMethod.POST))
	def importModules = {
		  moduleService.importData
		  "sysadmin/importdone"
	}
  
}