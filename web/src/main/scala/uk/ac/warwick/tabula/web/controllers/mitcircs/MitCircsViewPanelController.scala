package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.ViewViewableCommandAudited
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/panel/{panel}"))
class MitCircsViewPanelController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable panel: MitigatingCircumstancesPanel) =
    new ViewViewableCommandAudited(Permissions.MitigatingCircumstancesSubmission.Read, mandatory(panel))

  @RequestMapping(method = Array(GET, HEAD))
  def view(@ModelAttribute("command") command: ViewViewableCommandAudited[MitigatingCircumstancesPanel]): Mav = {
    val panel = command.apply()
    Mav("mitcircs/panel/view", "panel" -> panel)
      .crumbs(
        MitCircsBreadcrumbs.Admin.HomeForYear(panel.department, panel.academicYear),
        MitCircsBreadcrumbs.Admin.Panel(panel, active = true),
      )
  }

}
