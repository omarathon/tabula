package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.ReviewMitCircsSubmissionCommand
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}"))
class MitCircsReviewSubmissionController extends BaseController with FormattedHtml {

  @ModelAttribute("command")
  def command(@PathVariable submission: MitigatingCircumstancesSubmission): ReviewMitCircsSubmissionCommand.Command =
    ReviewMitCircsSubmissionCommand(mandatory(Option(submission).filterNot(_.isDraft).orNull))

  @RequestMapping
  def render(@ModelAttribute("command") cmd: ReviewMitCircsSubmissionCommand.Command, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    val result = cmd.apply()
    Mav("mitcircs/review",
      "submission" -> result.submission,
      "reasonableAdjustments" -> result.reasonableAdjustments,
      "reasonableAdjustmentsNotes" -> result.reasonableAdjustmentsNotes,
      "formattedReasonableAdjustmentsNotes" -> formattedHtml(result.reasonableAdjustmentsNotes),
      "otherMitigatingCircumstancesSubmissions" -> result.otherMitigatingCircumstancesSubmissions,
      "relevantExtensions" -> result.relevantExtensions,
    ).crumbs(
      MitCircsBreadcrumbs.Admin.Home(submission.department),
      MitCircsBreadcrumbs.Admin.Review(submission, active = true),
    )
  }

}
