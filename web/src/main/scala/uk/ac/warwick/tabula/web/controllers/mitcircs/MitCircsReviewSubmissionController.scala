package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}"))
class MitCircsReviewSubmissionController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable submission: MitigatingCircumstancesSubmission): ViewViewableCommand[MitigatingCircumstancesSubmission] =
    new ViewViewableCommand(Permissions.MitigatingCircumstancesSubmission.Manage, mandatory(submission))

  @RequestMapping
  def render(@ModelAttribute("command") cmd: Appliable[MitigatingCircumstancesSubmission], @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/review", "submission" -> cmd.apply())
      .crumbs(
        MitCircsBreadcrumbs.Admin.Home(submission.department),
        MitCircsBreadcrumbs.Admin.Review(submission, active = true),
      )
  }

}
