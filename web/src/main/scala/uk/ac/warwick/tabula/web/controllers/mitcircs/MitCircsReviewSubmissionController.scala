package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.ReviewMitCircsSubmissionCommand
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.data.model.mitcircs.{MitigatingCircumstancesPanel, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController
import MitCircsReviewPanelSubmissionController._

abstract class AbstractMitCircsReviewSubmissionController extends BaseController with FormattedHtml {
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

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}"))
class MitCircsReviewSubmissionController extends AbstractMitCircsReviewSubmissionController

object MitCircsReviewPanelSubmissionController {
  case class Pagination(
    current: MitigatingCircumstancesSubmission,
    index: Int,
    total: Int,
    previous: Option[MitigatingCircumstancesSubmission],
    next: Option[MitigatingCircumstancesSubmission],
  )
}

@Controller
@RequestMapping(Array("/mitcircs/panel/{panel}/submission/{submission}"))
class MitCircsReviewPanelSubmissionController extends AbstractMitCircsReviewSubmissionController {
  @ModelAttribute("pagination")
  def pagination(@PathVariable panel: MitigatingCircumstancesPanel, @PathVariable submission: MitigatingCircumstancesSubmission): Pagination = {
    mustBeLinked(submission, panel)

    val allSubmissions = panel.submissions.toIndexedSeq.sortBy(_.key)
    val currentIndex = allSubmissions.indexOf(submission)

    Pagination(
      current = submission,
      index = (currentIndex + 1),
      total = allSubmissions.size,
      previous = if (currentIndex > 0) Some(allSubmissions(currentIndex - 1)) else None,
      next = if (currentIndex < (allSubmissions.size - 1)) Some(allSubmissions(currentIndex + 1)) else None,
    )
  }
}