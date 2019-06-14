package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.mitcircs.submission.MitCircsRecordOutcomesCommand
import uk.ac.warwick.tabula.data.model.mitcircs.{MitCircsExamBoardRecommendation, MitigatingCircumstancesGrading, MitigatingCircumstancesRejectionReason, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}/outcomes"))
class MitCircsRecordOutcomesController extends BaseController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable submission: MitigatingCircumstancesSubmission, user: CurrentUser): MitCircsRecordOutcomesCommand.Command =
    MitCircsRecordOutcomesCommand(mandatory(Option(submission).filter(_.canRecordOutcomes).orNull), user.apparentUser)

  @RequestMapping
  def form(@PathVariable submission: MitigatingCircumstancesSubmission, currentUser: CurrentUser): Mav = {
    Mav("mitcircs/submissions/record_outcomes", Map(
      "boardRecommendations" -> MitCircsExamBoardRecommendation.values,
      "outcomeGrading" -> MitigatingCircumstancesGrading.values,
      "rejectionReasons" -> MitigatingCircumstancesRejectionReason.values,
    ))
  }

  @RequestMapping(method = Array(POST))
  def save(
    @Valid @ModelAttribute("command") cmd: MitCircsRecordOutcomesCommand.Command,
    errors: Errors,
    @PathVariable submission: MitigatingCircumstancesSubmission,
    currentUser: CurrentUser
  ): Mav = {
    if (errors.hasErrors) form(submission, currentUser)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Admin.review(submission))
    }
  }

}
