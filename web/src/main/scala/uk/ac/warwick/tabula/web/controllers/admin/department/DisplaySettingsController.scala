package uk.ac.warwick.tabula.web.controllers.admin.department

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.admin.department.DisplaySettingsCommand
import uk.ac.warwick.tabula.data.model.{CourseType, Department, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringRelationshipServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.DepartmentScopedController
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}

@Controller
@RequestMapping(Array("/admin/department/{department}/settings/display"))
class DisplaySettingsController extends AdminController
  with DepartmentScopedController
  with AutowiringUserSettingsServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringMaintenanceModeServiceComponent
  with AutowiringRelationshipServiceComponent {

  validatesSelf[SelfValidating]

  @ModelAttribute("displaySettingsCommand")
  def displaySettingsCommand(@PathVariable department: Department): DisplaySettingsCommand.Command =
    DisplaySettingsCommand(mandatory(department))

  @ModelAttribute("allRelationshipTypes") def allRelationshipTypes: Seq[StudentRelationshipType] = relationshipService.allStudentRelationshipTypes

  override val departmentPermission: Permission = DisplaySettingsCommand.RequiredPermission

  @ModelAttribute("activeDepartment")
  override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

  @ModelAttribute("expectedCourseTypes")
  def expectedCourseTypes: Seq[CourseType] = Seq(CourseType.UG, CourseType.PGT, CourseType.PGR, CourseType.Foundation, CourseType.PreSessional)

  @ModelAttribute("returnTo")
  def returnTo: String = getReturnTo("")

  @ModelAttribute("breadcrumbs")
  def breadcrumbs(@PathVariable department: Department): Seq[BreadCrumb] = Seq(Breadcrumbs.Department(department))

  @RequestMapping
  def formView: String = "admin/display-settings"

  @PostMapping
  def saveSettings(
    @Valid @ModelAttribute("displaySettingsCommand") cmd: DisplaySettingsCommand.Command,
    errors: Errors,
    @PathVariable department: Department
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      formView
    } else {
      cmd.apply()
      RedirectFlashing(Routes.admin.department(department), "flash__success" -> "flash.departmentSettings.saved")
    }
}
