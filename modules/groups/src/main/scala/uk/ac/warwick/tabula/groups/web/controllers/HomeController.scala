package uk.ac.warwick.tabula.groups.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.spring.Wire

@Controller class HomeController extends BaseController {
	var moduleService = Wire[ModuleAndDepartmentService]
	
	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		val ownedDepartments = moduleService.departmentsOwnedBy(user)
		val ownedModules = moduleService.modulesManagedBy(user)
		
		Mav("home/view",
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.department })
	}
	
}