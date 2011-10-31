package uk.ac.warwick.courses.web.controllers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import uk.ac.warwick.courses.actions._
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.web.forms.AddAssignmentForm
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import uk.ac.warwick.courses.services.AssignmentService

/**
 * Screens for department and module admins.
 */

@Controller
class AdminController extends Controllerism  {

  @Autowired var moduleService:ModuleAndDepartmentService =_
  
  @RequestMapping(Array("/admin/"))
  def homeScreen(user:CurrentUser) = 
    	Mav("admin/home", 
  			"ownedDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions)
  			)
  
  @RequestMapping(Array("/admin/department/{dept}/"))
  def adminDepartment(@PathVariable dept:Department, user:CurrentUser):ModelAndView = {
    securityService.check(user, Manage(dept))
    Mav("admin/department", 
        "department" -> dept,
        "modules" -> dept.modules)
  }

}

@Controller
class AdminAddAssignmentController extends Controllerism  {
  @Autowired var assignmentService:AssignmentService =_
  
  @ModelAttribute("addAssignment") def addAssignmentForm(@PathVariable module:Module) = 
		  new AddAssignmentForm(module)
  
  @RequestMapping(value=Array("/admin/module/{module}/assignments/new"), method=Array(RequestMethod.GET))
  def addAssignmentForm(user:CurrentUser,
		  @PathVariable module:Module,
		  @ModelAttribute("addAssignment") form:AddAssignmentForm,
		  errors:Errors):ModelAndView = {
    securityService.check(user, Manage(module.department))
    Mav("admin/assignments/new", 
        "department" -> module.department,
        "module" -> module)
  }
  
  @RequestMapping(value=Array("/admin/module/{module}/assignments/new"), method=Array(RequestMethod.POST))
  def addAssignmentSubmit(user:CurrentUser,
		  @PathVariable module:Module,
		  @Valid @ModelAttribute("addAssignment") form:AddAssignmentForm,
		  errors:Errors):ModelAndView = {
    securityService.check(user, Manage(module.department))
    if (errors.hasErrors) {
      addAssignmentForm(user,module,form,errors)
    } else {
      assignmentService.create(form)
      Mav("redirect:/admin/department/"+module.department.code+"/#module-"+module.code)
    }
  }
  
}