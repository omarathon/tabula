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
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs
import uk.ac.warwick.tabula.web.controllers.profiles.profile.AbstractViewProfileController

@Controller
@RequestMapping(value = Array("/profiles/view/{student}/personalcircs/mitcircs/sensitiveevidence/{submission}"))
class MitCircsSensitiveEvidenceController extends AbstractViewProfileController {

  validatesSelf[SelfValidating]
  type Command = Appliable[MitigatingCircumstancesSubmission] with MitCircsSensitiveEvidenceState with SelfValidating

  @ModelAttribute("command") def command(
    @PathVariable submission: MitigatingCircumstancesSubmission,
    @PathVariable student: StudentMember,
    user: CurrentUser
  ): Command = {
    mustBeLinked(submission, student)
    MitCircsSensitiveEvidenceCommand(mandatory(submission), user.apparentUser)
  }

  @RequestMapping
  def form(@ModelAttribute("student") student: StudentMember, @PathVariable submission: MitigatingCircumstancesSubmission): Mav = {
    mustBeLinked(mandatory(submission), mandatory(student))
    Mav("mitcircs/submissions/sensitive_evidence")
      .crumbs(breadcrumbsStudent(activeAcademicYear, student.mostSignificantCourse, ProfileBreadcrumbs.Profile.PersonalCircumstances): _*)
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