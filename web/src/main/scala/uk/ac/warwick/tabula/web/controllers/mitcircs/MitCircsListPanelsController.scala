package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.mitcircs.ListMitCircsPanelsCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController, DepartmentScopedController}

abstract class AbstractMitCircsListPanelsController
  extends BaseController
    with DepartmentScopedController
    with AcademicYearScopedController
    with AutowiringModuleAndDepartmentServiceComponent
    with AutowiringUserSettingsServiceComponent
    with AutowiringMaintenanceModeServiceComponent {

  hideDeletedItems

  override val departmentPermission: Permission = ListMitCircsPanelsCommand.RequiredPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] =
    retrieveActiveDepartment(Option(department))

  @ModelAttribute("command")
  def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): ListMitCircsPanelsCommand.Command =
    ListMitCircsPanelsCommand(mandatory(department), activeAcademicYear.getOrElse(AcademicYear.now()))

  @RequestMapping
  def results(@ModelAttribute("command") command: ListMitCircsPanelsCommand.Command, errors: Errors): Mav =
    Mav("mitcircs/admin/panels", "panels" -> command.apply()).noLayout()

}

@Controller
@RequestMapping(value = Array("/mitcircs/admin/{department}/panels"))
class MitCircsListPanelsController extends AbstractMitCircsListPanelsController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] =
    retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/mitcircs/admin/{department}/{academicYear:\\d{4}}/panels"))
class MitCircsListPanelsForYearController extends AbstractMitCircsListPanelsController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

}
