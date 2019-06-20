package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.submission.EditMitCircsPanelCommand
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/panel/{panel}/edit"))
class MitCircsEditPanelController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable panel: MitigatingCircumstancesPanel): EditMitCircsPanelCommand.Command =
    EditMitCircsPanelCommand(mandatory(panel))

  @RequestMapping(params = Array("!submit"))
  def form(@PathVariable panel: MitigatingCircumstancesPanel): Mav =
    Mav("mitcircs/panel/edit")
      .crumbs(
        MitCircsBreadcrumbs.Admin.HomeForYear(panel.department, panel.academicYear),
        MitCircsBreadcrumbs.Admin.ListPanels(panel.department, panel.academicYear),
        MitCircsBreadcrumbs.Admin.Panel(panel),
      )

  @PostMapping(params = Array("submit"))
  def submit(@ModelAttribute("command") command: EditMitCircsPanelCommand.Command, errors: Errors, @PathVariable panel: MitigatingCircumstancesPanel): Mav =
    if (errors.hasErrors) form(panel)
    else {
      val p = command.apply()
      RedirectForce(Routes.Admin.Panels.view(p))
    }

}
