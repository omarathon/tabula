package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.tabula.commands.sysadmin.DepartmentMandatoryPermissionsCommand
import uk.ac.warwick.tabula.data.model.CourseType
import uk.ac.warwick.tabula.services.scheduling.AutowiringSchedulerComponent
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.helpers.SchedulingHelpers._
import uk.ac.warwick.tabula.services.scheduling.jobs.DepartmentMandatoryPermissionsWarningJob

@Controller
@RequestMapping(Array("/sysadmin/departments/mandatory-permissions"))
class DepartmentMandatoryPermissionsController extends BaseSysadminController
  with AutowiringSchedulerComponent {

  @ModelAttribute("command")
  def command: DepartmentMandatoryPermissionsCommand.Command = DepartmentMandatoryPermissionsCommand()

  @ModelAttribute("courseTypes")
  def courseTypes: Seq[CourseType] = CourseType.all

  @RequestMapping
  def summary(@ModelAttribute("command") command: DepartmentMandatoryPermissionsCommand.Command): Mav =
    Mav("sysadmin/departments/mandatory-permissions", "departmentInfo" -> command.apply())
      .crumbs(SysadminBreadcrumbs.Departments.Home)

  @PostMapping
  def scheduleJob(implicit redirectAttributes: RedirectAttributes): String = {
    scheduler.scheduleNow[DepartmentMandatoryPermissionsWarningJob]()
    RedirectFlashing(Routes.sysadmin.Departments.mandatoryPermissions, "flash__success" -> "flash.sysadmin.departmentMandatoryPermissions.scheduled")
  }

}
