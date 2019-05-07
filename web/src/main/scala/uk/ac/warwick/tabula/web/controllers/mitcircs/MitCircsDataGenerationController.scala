package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.MitCircsDataGenerationCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{BaseController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Profile(Array("sandbox"))
@Controller
@RequestMapping(Array("/mitcircs/admin/{department}/data-generation"))
class MitCircsDataGenerationController
  extends BaseController
    with DepartmentScopedController
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringUserSettingsServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  validatesSelf[SelfValidating]

  override val departmentPermission: Permission = MitCircsDataGenerationCommand.RequiredPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    retrieveActiveDepartment(Option(department))

  @ModelAttribute("command")
  def command(@PathVariable department: Department): MitCircsDataGenerationCommand.Command =
    MitCircsDataGenerationCommand(mandatory(department))

  @RequestMapping
  def form(@PathVariable department: Department): Mav =
    Mav("mitcircs/data-generation/form")
      .crumbs(MitCircsBreadcrumbs.Admin.Home(department))

  @PostMapping
  def generate(@Valid @ModelAttribute("command") command: MitCircsDataGenerationCommand.Command, errors: Errors): Mav =
    if (errors.hasErrors) form(command.department)
    else {
      command.apply()
      RedirectForce(Routes.mitcircs.Admin.home(command.department))
    }

}
