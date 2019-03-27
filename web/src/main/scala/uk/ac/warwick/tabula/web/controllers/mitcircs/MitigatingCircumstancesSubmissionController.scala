package uk.ac.warwick.tabula.web.controllers.mitcircs

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.mitcircs.submission.{CreateMitCircsSubmissionCommand, CreateMitCircsSubmissionState}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.mitcircs.{IssueType, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.mitcircs.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller
@RequestMapping(value = Array("/mitcircs/profile/{student}/new"))
class MitigatingCircumstancesSubmissionController extends BaseController {

  validatesSelf[SelfValidating]

  type CreateCommand = Appliable[MitigatingCircumstancesSubmission] with CreateMitCircsSubmissionState with SelfValidating

  @ModelAttribute("createMitCircsCommand") def create(@PathVariable student: StudentMember, user: CurrentUser): CreateCommand =
    CreateMitCircsSubmissionCommand(student, user.apparentUser)

  @RequestMapping
  def form(@ModelAttribute("createMitCircsCommand") cmd: CreateCommand, @PathVariable student: StudentMember): Mav = {
    Mav("mitcircs/submissions/create", Map("issueTypes" -> IssueType.values))
  }

  @RequestMapping(method = Array(POST))
  def save(@Valid @ModelAttribute("createMitCircsCommand") cmd: CreateCommand, errors: Errors, @PathVariable student: StudentMember): Mav = {
    if (errors.hasErrors) form(cmd, student)
    else {
      val submission = cmd.apply()
      RedirectForce(Routes.Student.home(student))
    }
  }

}
