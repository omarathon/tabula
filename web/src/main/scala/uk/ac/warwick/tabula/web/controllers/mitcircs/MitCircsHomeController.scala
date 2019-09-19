package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.mitcircs.MitCircsHomeCommand
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{BaseController, DepartmentsAndModulesWithPermission}

@Controller
@RequestMapping(value = Array("/mitcircs"))
class MitCircsHomeController extends BaseController with DepartmentsAndModulesWithPermission with AutowiringModuleAndDepartmentServiceComponent {

  @ModelAttribute("command")
  def command(currentUser: CurrentUser): MitCircsHomeCommand.Command = MitCircsHomeCommand(currentUser)

  @RequestMapping
  def render(@ModelAttribute("command") command: MitCircsHomeCommand.Command, currentUser: CurrentUser): Mav = {
    val info = command.apply()
    if (currentUser.isStudent && info.isEmpty) Redirect(Routes.Profile.PersonalCircumstances())
    else Mav("mitcircs/home", "departments" -> info.mcoDepartments, "panels" -> info.panels)
  }

}