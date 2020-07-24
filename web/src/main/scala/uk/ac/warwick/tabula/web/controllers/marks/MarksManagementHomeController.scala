package uk.ac.warwick.tabula.web.controllers.marks

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.marks.MarksManagementHomeCommand
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{BaseController, DepartmentsAndModulesWithPermission}

@Controller
@RequestMapping(value = Array("/marks"))
class MarksManagementHomeController
  extends BaseController
    with DepartmentsAndModulesWithPermission
    with AutowiringModuleAndDepartmentServiceComponent {

  @ModelAttribute("command")
  def command(currentUser: CurrentUser): MarksManagementHomeCommand.Command = MarksManagementHomeCommand(currentUser)

  @RequestMapping
  def render(@ModelAttribute("command") command: MarksManagementHomeCommand.Command): Mav = {
    val info = command.apply()
    Mav("marks/home",
      "adminDepartments" -> info.adminDepartments,
      "moduleManagerDepartments" -> info.moduleManagerDepartments,
    )
  }
}
