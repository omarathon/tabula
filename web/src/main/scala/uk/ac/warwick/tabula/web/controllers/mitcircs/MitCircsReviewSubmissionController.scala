package uk.ac.warwick.tabula.web.controllers.mitcircs

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.mitcircs.ReviewMitCircsSubmissionCommand
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(Array("/mitcircs/submission/{submission}"))
class MitCircsReviewSubmissionController extends BaseController {

  @ModelAttribute("command")
  def command(@PathVariable submission: MitigatingCircumstancesSubmission): ReviewMitCircsSubmissionCommand.Command =
    ReviewMitCircsSubmissionCommand(mandatory(submission))

  @RequestMapping
  def render(@ModelAttribute("command") cmd: ReviewMitCircsSubmissionCommand.Command, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    Mav("mitcircs/review", "submission" -> cmd.apply())
      .crumbs(
        MitCircsBreadcrumbs.Admin.Home(submission.department),
        MitCircsBreadcrumbs.Admin.Review(submission, active = true),
      )
  }

}
