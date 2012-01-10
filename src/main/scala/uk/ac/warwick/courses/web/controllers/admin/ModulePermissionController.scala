package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.stereotype.Controller
import java.util.{List=>JList}
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.web.Mav

@Controller
class ModulePermissionController extends BaseController {
	
	def addPermission(module:Module, usercode:JList[String]) : Mav = {
		null
	}
	
	def removePermission(module:Module, usercode:JList[String]) : Mav = {
		null
	}
	
}