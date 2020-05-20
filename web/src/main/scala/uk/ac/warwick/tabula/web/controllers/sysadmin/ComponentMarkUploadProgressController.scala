package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.sysadmin._
import uk.ac.warwick.tabula.web.Mav


@Controller
@RequestMapping(value = Array("/sysadmin/marksmanagement"))
class ComponentMarkUploadProgressController extends BaseSysadminController {

  @ModelAttribute("componentMarkUploadProgressCommand") def componentMarkUploadProgressCommand = ComponentMarkUploadProgressCommand()

  @RequestMapping(method = Array(GET, HEAD))
  def check(@ModelAttribute("componentMarkUploadProgressCommand") cmd: Appliable[CheckStudentRelationshipImport]): Mav =
    Mav("sysadmin/marksmanagement/home", "result" -> cmd.apply(), "academicYear" -> AcademicYear.now()).crumbs(SysadminBreadcrumbs.MarksManagement.Home)

}
