package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.data.model.Department
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.actions._

@Controller
class DepartmentController extends Controllerism {

  @Autowired var moduleService:ModuleAndDepartmentService =_
  
//  @RequestMapping(Array("/admin/"))
//  def homeScreen(user:CurrentUser) = 
//    	Mav("admin/home", 
//  			"ownedDepartments" -> moduleService.departmentsOwnedBy(user)
//  			)

  
  @RequestMapping(Array("/admin/{deptcode}/"))
  def adminDepartment(@PathVariable("deptcode") dept:Department, user:CurrentUser):ModelAndView = {
    securityService.check(user, Manage(dept))
    Mav("admin/department", 
        "department" -> dept,
        "modules" -> dept.modules)
  }
  
  
}