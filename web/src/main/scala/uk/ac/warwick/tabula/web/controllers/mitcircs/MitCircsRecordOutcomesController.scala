package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
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
  def form(@PathVariable submission: MitigatingCircumstancesSubmission, currentUser: CurrentUser, @RequestParam(defaultValue = "false") fromPanel: Boolean): Mav = {
    val mav = Mav("mitcircs/submissions/record_outcomes", Map(
      "boardRecommendations" -> MitCircsExamBoardRecommendation.values,
      "outcomeGrading" -> MitigatingCircumstancesGrading.values,
      "rejectionReasons" -> MitigatingCircumstancesRejectionReason.values,
      "fromPanel" -> fromPanel,
      "isPanelChair" -> submission.panel.exists(_.chair.contains(currentUser.apparentUser))
    ))

    if (fromPanel && submission.panel.nonEmpty)
      mav.crumbs(
        MitCircsBreadcrumbs.Admin.Home(submission.department),
        MitCircsBreadcrumbs.Admin.Panel(submission.panel.get),
        MitCircsBreadcrumbs.Admin.ReviewPanel(submission),
        MitCircsBreadcrumbs.Admin.PanelOutcomes(submission, active = true),
      )
    else
      mav.crumbs(
        MitCircsBreadcrumbs.Admin.Home(submission.department),
        MitCircsBreadcrumbs.Admin.Review(submission),
        MitCircsBreadcrumbs.Admin.PanelOutcomes(submission, active = true),
      )
  }

  @RequestMapping(method = Array(POST))
  def save(
    @Valid @ModelAttribute("command") cmd: MitCircsRecordOutcomesCommand.Command,
    errors: Errors,
    @PathVariable submission: MitigatingCircumstancesSubmission,
    currentUser: CurrentUser,
    @RequestParam(defaultValue = "false") fromPanel: Boolean
  ): Mav =
    if (errors.hasErrors) form(submission, currentUser, fromPanel)
    else {
      val submission = cmd.apply()
      if (fromPanel && submission.panel.nonEmpty) {
        RedirectForce(Routes.Admin.Panels.review(submission))
      } else {
        RedirectForce(Routes.Admin.review(submission))
      }
    }

}
