package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.submission.{MitCircsRecordAcuteOutcomesCommand, MitCircsRecordOutcomesCommand}
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesAcuteOutcome, MitigatingCircumstancesGrading, MitigatingCircumstancesRejectionReason, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/acuteoutcomes"))
class MitCircsRecordAcuteOutcomesController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): MitCircsRecordAcuteOutcomesCommand.Command =
    MitCircsRecordAcuteOutcomesCommand(mandatory(Option(submission).filter(_.canRecordAcuteOutcomes).orNull), user.apparentUser)

  @RequestMapping
  def form(@PathVariable submission: MitigatingCircumstancesSubmission, currentUser: CurrentUser): Mav =
    Mav("mitcircs/submissions/record_acute_outcomes", Map(
      "acuteOutcomes" -> MitigatingCircumstancesAcuteOutcome.values,
      "outcomeGrading" -> MitigatingCircumstancesGrading.values,
      "rejectionReasons" -> MitigatingCircumstancesRejectionReason.values,
    )).crumbs(
      MitCircsBreadcrumbs.Admin.Home(submission.department),
      MitCircsBreadcrumbs.Admin.Review(submission),
      MitCircsBreadcrumbs.Admin.AcuteOutcomes(submission, active = true),
    )

  @RequestMapping(method = Array(POST))
  def save(
    @Valid @ModelAttribute("command") cmd: MitCircsRecordOutcomesCommand.Command,
    errors: Errors,
    @PathVariable submission: MitigatingCircumstancesSubmission,
    currentUser: CurrentUser
  ): Mav =
    if (errors.hasErrors) form(submission, currentUser)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Admin.review(submission))
    }

}
