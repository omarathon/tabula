package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.mitcircs.{AdminHomeCommand, AdminHomeInformation}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{BaseController, DepartmentsAndModulesWithPermission}

@Controller
@RequestMapping(value = Array("/mitcircs"))
class AdminDeptListController extends BaseController with DepartmentsAndModulesWithPermission with AutowiringModuleAndDepartmentServiceComponent {

  @RequestMapping
  def render: Mav = {
    val departments = allDepartmentsForPermission(user, Permissions.MitigatingCircumstancesSubmission.Manage)
    if(departments.size == 1) Redirect(Routes.Admin.home(departments.head))
    else {
      Mav("mitcircs/admin/department-list", "departments" -> departments)
    }
  }

}

@Controller
@RequestMapping(value = Array("/mitcircs/admin/{department}"))
class AdminDeptController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable department: Department): Appliable[AdminHomeInformation] = {
    AdminHomeCommand(mandatory(department))
  }

  @RequestMapping
  def render(@ModelAttribute("command") cmd: Appliable[AdminHomeInformation], @PathVariable department: Department): Mav = {
    val info = cmd.apply()
    Mav("mitcircs/admin/home", "submissions" -> info.submissions)
  }

}
