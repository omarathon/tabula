package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.submission.EditMitCircsPanelCommand
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesPanel
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

import scala.jdk.CollectionConverters._

@Controller
@RequestMapping(value = Array("/mitcircs/panel/{panel}/edit"))
class MitCircsEditPanelController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable panel: MitigatingCircumstancesPanel): EditMitCircsPanelCommand.Command =
    EditMitCircsPanelCommand(mandatory(panel))

  @RequestMapping(params = Array("!submit"))
  def form(@ModelAttribute("command") command: EditMitCircsPanelCommand.Command, @PathVariable panel: MitigatingCircumstancesPanel): Mav = {
    command.populate()
    val (thisPanel, others) = command.submissions.asScala.partition(_.panel.exists(_ == panel))
    val (hasPanel, noPanel) = others.partition(_.panel.isDefined)

    Mav("mitcircs/panel/edit",
      "thisPanel" -> thisPanel,
      "hasPanel" -> hasPanel,
      "noPanel" -> noPanel,
    ).crumbs(
      MitCircsBreadcrumbs.Admin.HomeForYear(panel.department, panel.academicYear),
      MitCircsBreadcrumbs.Admin.ListPanels(panel.department, panel.academicYear),
      MitCircsBreadcrumbs.Admin.Panel(panel),
    )
  }

  @PostMapping(params = Array("submit"))
  def submit(@Valid @ModelAttribute("command") command: EditMitCircsPanelCommand.Command, errors: Errors, @PathVariable panel: MitigatingCircumstancesPanel): Mav =
    if (errors.hasErrors) form(command, panel)
    else {
      val p = command.apply()
      RedirectForce(Routes.Admin.Panels.view(p))
    }

}
