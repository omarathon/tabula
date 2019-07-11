package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsApproveOutcomesCommand
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(value = Array("/mitcircs/submission/{submission}/approve"))
class MitCircsApproveOutcomesController extends BaseController {

  @ModelAttribute("command") def command(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): MitCircsApproveOutcomesCommand.Command =
    MitCircsApproveOutcomesCommand(mandatory(submission), user.apparentUser)

  @RequestMapping
  def form(@ModelAttribute("student") student: StudentMember, @PathVariable submission: MitigatingCircumstancesSubmission, @RequestParam(defaultValue = "false") fromPanel: Boolean): Mav =
    Mav("mitcircs/submissions/approve_outcomes", "fromPanel" -> fromPanel)
      .noLayout()

  @RequestMapping(method = Array(POST))
  def save(@ModelAttribute("command") cmd: MitCircsApproveOutcomesCommand.Command, @PathVariable submission: MitigatingCircumstancesSubmission, @RequestParam(defaultValue = "false") fromPanel: Boolean): Mav = {
    cmd.apply()
    Mav(new JSONView(Map("success" -> true)))
  }

}