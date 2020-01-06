package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.sysadmin.DepartmentMandatoryPermissionsCommand
import uk.ac.warwick.tabula.data.model.CourseType
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/departments/mandatory-permissions"))
class DepartmentMandatoryPermissionsController extends BaseSysadminController {

  @ModelAttribute("command")
  def command: DepartmentMandatoryPermissionsCommand.Command = DepartmentMandatoryPermissionsCommand()

  @ModelAttribute("courseTypes")
  def courseTypes: Seq[CourseType] = CourseType.all

  @RequestMapping
  def summary(@ModelAttribute("command") command: DepartmentMandatoryPermissionsCommand.Command): Mav =
    Mav("sysadmin/departments/mandatory-permissions", "departmentInfo" -> command.apply())
      .crumbs(SysadminBreadcrumbs.Departments.Home)

}
