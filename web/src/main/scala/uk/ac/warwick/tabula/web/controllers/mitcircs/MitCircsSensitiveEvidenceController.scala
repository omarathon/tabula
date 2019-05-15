package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.mitcircs.submission.{MitCircsSensitiveEvidenceCommand, MitCircsSensitiveEvidenceState}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/submission/{submission}/sensitiveevidence"))
class MitCircsSensitiveEvidenceController extends BaseController {

  validatesSelf[SelfValidating]
  type Command = Appliable[MitigatingCircumstancesSubmission] with MitCircsSensitiveEvidenceState with SelfValidating

  @ModelAttribute("command") def command(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): Command =
    MitCircsSensitiveEvidenceCommand(mandatory(submission), user.apparentUser)

  @RequestMapping
  def form(@ModelAttribute("student") student: StudentMember, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/submissions/sensitive_evidence")
      .crumbs(
        MitCircsBreadcrumbs.Admin.Home(submission.department),
        MitCircsBreadcrumbs.Admin.Review(submission),
        MitCircsBreadcrumbs.Admin.SensitiveEvidence(submission, active = true),
      )
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("command") cmd: Command, errors: Errors, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    if (errors.hasErrors) form(submission.student, submission)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Admin.review(submission))
    }
  }

}