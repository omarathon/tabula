package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.MitCircsViewPanelCommand
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/panel/{panel}"))
class MitCircsViewPanelController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable panel: MitigatingCircumstancesPanel): MitCircsViewPanelCommand.Command = MitCircsViewPanelCommand(mandatory(panel))

  @RequestMapping(method = Array(GET, HEAD))
  def view(@ModelAttribute("command") command: MitCircsViewPanelCommand.Command): Mav = {
    val info = command.apply()
    Mav("mitcircs/panel/view", "panel" -> info.panel, "submissionStages" -> info.submissionStages)
      .crumbs(
        MitCircsBreadcrumbs.Admin.HomeForYear(info.panel.department, info.panel.academicYear),
        MitCircsBreadcrumbs.Admin.ListPanels(info.panel.department, info.panel.academicYear),
        MitCircsBreadcrumbs.Admin.Panel(info.panel, active = true)
      )
  }

}
